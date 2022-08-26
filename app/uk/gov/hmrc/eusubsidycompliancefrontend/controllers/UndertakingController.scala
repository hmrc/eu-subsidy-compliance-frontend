/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.data.OptionT
import cats.implicits._
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailTemplate, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class UndertakingController @Inject() (
                                        mcc: MessagesControllerComponents,
                                        actionBuilders: ActionBuilders,
                                        override val store: Store,
                                        override val escService: EscService,
                                        emailService: EmailService,
                                        emailVerificationService: EmailVerificationService,
                                        timeProvider: TimeProvider,
                                        auditService: AuditService,
                                        aboutUndertakingPage: AboutUndertakingPage,
                                        undertakingSectorPage: UndertakingSectorPage,
                                        confirmEmailPage: ConfirmEmailPage,
                                        addBusinessEntityEoriPage: BusinessEntityEoriPage,
                                        inputEmailPage: InputEmailPage,
                                        addBusinessPage: AddBusinessPage,
                                        cyaPage: UndertakingCheckYourAnswersPage,
                                        confirmationPage: ConfirmationPage,
                                        amendUndertakingPage: AmendUndertakingPage,
                                        disableUndertakingWarningPage: DisableUndertakingWarningPage,
                                        disableUndertakingConfirmPage: DisableUndertakingConfirmPage,
                                        undertakingDisabledPage: UndertakingDisabledPage,
                                        removeBusinessPage: RemoveBusinessPage,
  )(implicit val appConfig: AppConfig, override val executionContext: ExecutionContext) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with FormHelpers {

  import actionBuilders._

  def firstEmptyPage: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).map { journey =>
      journey.firstEmpty
        .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))(identity)
    }
  }

  def getAboutUndertaking: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val form = journey.about.value.fold(aboutUndertakingForm)(name => aboutUndertakingForm.fill(FormValues(name)))
      Ok(aboutUndertakingPage(form, journey.previous)).toFuture
    }
  }

  def postAboutUndertaking: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(_) =>
        aboutUndertakingForm
          .bindFromRequest()
          .fold(
            _ => throw new IllegalStateException("Unexpected form submission"),
            _ => {
              for {
                // We store the EORI in the undertaking name field.
                updatedUndertakingJourney <- store.update[UndertakingJourney](_.setUndertakingName(eori))
                redirect <- updatedUndertakingJourney.next
              } yield redirect
            }
          )
      case None => handleMissingSessionData("Undertaking Journey")

    }
  }

  def getSector: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        if (!journey.isEligibleForStep) {
          Redirect(journey.previous).toFuture
        } else {
          val form = journey.sector.value.fold(undertakingSectorForm)(sector =>
            undertakingSectorForm.fill(FormValues(sector.id.toString))
          )
          Ok(
            undertakingSectorPage(
              form,
              journey.previous,
              journey.about.value.getOrElse("")
            )
          ).toFuture
        }
      }
    }
  }

  def postSector: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors => {
            val result = for {
              undertakingName <- journey.about.value.toContext
            } yield BadRequest(undertakingSectorPage(errors, journey.previous, undertakingName))
            result
              .fold(handleMissingSessionData("Undertaking Journey"))(identity)
              .toContext
          },
          form =>
              store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
                .flatMap(_.next)
                .toContext
        )
    }
  }

  def getConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        if (journey.isEligibleForStep) {
          emailVerificationService.getEmailVerification(request.eoriNumber).flatMap {
            case Some(verifiedEmail) =>
              Ok(confirmEmailPage(optionalEmailForm, EmailAddress(verifiedEmail.email), routes.UndertakingController.getSector().url)).toFuture
            case None => emailService.retrieveEmailByEORI(request.eoriNumber) map { response =>
              response.emailType match {
                case EmailType.VerifiedEmail =>
                  Ok(confirmEmailPage(optionalEmailForm, response.emailAddress.get, routes.UndertakingController.getSector().url))
                case _ =>
                  Ok(inputEmailPage(emailForm, routes.UndertakingController.getSector().url))
              }
            }
          }
        } else {
          Redirect(journey.previous).toFuture
        }
      }
    }
  }

  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val verifiedEmail = for {
      stored <- emailVerificationService.getEmailVerification(request.eoriNumber)
      cds <- emailService.retrieveEmailByEORI(request.eoriNumber)
      result = if(stored.isDefined) stored.get.email.some else cds match {
        case RetrieveEmailResponse(VerifiedEmail, Some(value)) => value.value.some
        case _ => Option.empty
      }
    } yield result

    verifiedEmail flatMap {
      case Some(email) =>
        optionalEmailForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(confirmEmailPage(errors, EmailAddress(email), routes.EligibilityController.getDoYouClaim().url)).toFuture,
            {
              case OptionalEmailFormInput("true", None) =>
                for {
                  _ <- emailVerificationService.addVerificationRequest(request.eoriNumber, email)
                  _ <- emailVerificationService.verifyEori(request.eoriNumber)
                  _ <- store.update[UndertakingJourney](_.setVerifiedEmail(email))
                } yield Redirect(routes.UndertakingController.getAddBusinessEntity())
              case OptionalEmailFormInput("false", Some(email)) => {
                for {
                  verificationId <- emailVerificationService.addVerificationRequest(request.eoriNumber, email)
                  verificationResponse <- emailVerificationService.verifyEmail(request.authorityId, email, verificationId)
                } yield emailVerificationService.emailVerificationRedirect(verificationResponse)
              }
              case _ => Redirect(routes.EligibilityController.getNotEligible()).toFuture
            }
          )
      case _ => emailForm.bindFromRequest().fold(
        errors => BadRequest(inputEmailPage(errors, routes.EligibilityController.getDoYouClaim().url)).toFuture,
        form => {
          for {
            verificationId <- emailVerificationService.addVerificationRequest(request.eoriNumber, form.value)
            verificationResponse <- emailVerificationService.verifyEmail(request.authorityId, form.value, verificationId)
          } yield emailVerificationService.emailVerificationRedirect(verificationResponse)
        }
      )
    }
  }

  def getVerifyEmail(verificationId: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        for {
          e <- emailVerificationService.approveVerificationRequest(request.eoriNumber, verificationId)
          wasSuccessful = e.getMatchedCount > 0
          redirect <- if(wasSuccessful) {
            for {
              stored <- emailVerificationService.getEmailVerification(request.eoriNumber)
              _ <- store.update[UndertakingJourney](_.setVerifiedEmail(stored.get.email))
              redirect <- if(wasSuccessful) journey.next else Future(Redirect(routes.UndertakingController.getConfirmEmail().url))
              } yield redirect
          } else Future(Redirect(routes.UndertakingController.getAboutUndertaking().url))
        } yield redirect
      case None => handleMissingSessionData("Undertaking Journey")
    }
  }

  def getAddBusinessEntity: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        if (journey.isEligibleForStep) {
          val form = journey.addBusiness.value
            .fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))
          Ok(
            addBusinessPage(
              form,
              UndertakingName(eori),
              journey.addBusiness.value.getOrElse(List.empty[EORI]),
              eori,
              journey.previous,
              routes.UndertakingController.postAddBusinessEntity(),
              eori => routes.UndertakingController.getRemoveBusinessEntity(eori)
            )
          ).toFuture
        } else {
          Redirect(journey.previous).toFuture
        }
      }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswer(form: FormValues) = {
      if (form.value.isTrue)
        Redirect(routes.UndertakingController.getAddBusinessEntityEori()).toFuture
      else Redirect(routes.AccountController.getAccountPage()).toFuture//check
    }

    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        addBusinessForm
          .bindFromRequest()
          .fold(
            errors => {
              BadRequest(
                addBusinessPage(
                  errors,
                  UndertakingName(eori),
                  journey.addBusiness.value.getOrElse(List.empty[EORI]),
                  eori,
                  journey.previous,
                  routes.UndertakingController.postAddBusinessEntity(),
                  eori => routes.UndertakingController.getRemoveBusinessEntity(eori)
                )).toFuture
            },
            handleValidAnswer
          )
      }
    }
  }

  def getAddBusinessEntityEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(_) =>
          Ok(addBusinessEntityEoriPage(eoriForm, routes.UndertakingController.getAddBusinessEntity().url, routes.UndertakingController.postAddBusinessEntityEori())).toFuture
      case _ => Redirect(routes.UndertakingController.getAddBusinessEntity()).toFuture
    }
  }

  def postAddBusinessEntityEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val businessEntityEori = "businessEntityEori"

    def getErrorResponse(errorMessageKey: String, previous: String, form: FormValues): Future[Result] =
      BadRequest(addBusinessEntityEoriPage(eoriForm.withError(businessEntityEori, errorMessageKey).fill(form), previous, routes.UndertakingController.postAddBusinessEntityEori())).toFuture

    def handleValidEori(form: FormValues, previous: Uri): Future[Result] = {
      if(form.value == eori) {
        getErrorResponse(s"businessEntityEori.eoriInUse", previous, form)
      } else {
        escService.retrieveUndertakingAndHandleErrors(EORI(form.value)).flatMap {
          case Right(Some(_)) =>
            getErrorResponse("businessEntityEori.eoriInUse", previous, form)
          case Left(_) =>
            getErrorResponse(s"error.$businessEntityEori.required", previous, form)
          case Right(None) =>
            store.update[UndertakingJourney](_.addBusinessEntity(EORI(form.value))).flatMap(_ => Redirect(routes.UndertakingController.getAddBusinessEntity()).toFuture)
        }
      }
    }

    processFormSubmission[UndertakingJourney] { _ =>
      eoriForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(addBusinessEntityEoriPage(errors, routes.UndertakingController.getAddBusinessEntity().url, routes.UndertakingController.postAddBusinessEntityEori())).toContext,
          form => {
            handleValidEori(form, routes.UndertakingController.getAddBusinessEntity().url).toContext
          }
        )
    }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.update[UndertakingJourney](_.removeBusinessEntity(EORI(eoriEntered))).map { _ =>
      Redirect(routes.UndertakingController.getAddBusinessEntity())
    }
  }

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async { implicit  request =>
    Ok(removeBusinessPage(removeBusinessForm, EORI(eoriEntered), routes.UndertakingController.postRemoveBusinessEntity(eoriEntered))).toFuture
  }


  def getCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- journey.verifiedEmail.value.toContext
          businessEntities <- journey.addBusiness.value.toContext
        } yield Ok(cyaPage(eori, undertakingSector, undertakingVerifiedEmail, businessEntities, journey.previous))
        result.fold(Redirect(journey.previous))(identity)
      case _ => Redirect(routes.UndertakingController.getAboutUndertaking()).toFuture
    }
  }

  def postCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("Invalid form submission"),
        form => {
          val result = for {
            updatedJourney <- store.update[UndertakingJourney](_.setUndertakingCYA(form.value.toBoolean)).toContext
            undertakingName <- updatedJourney.about.value.toContext
            undertakingSector <- updatedJourney.sector.value.toContext
            undertaking = UndertakingCreate(
              name = UndertakingName(undertakingName),
              industrySector = undertakingSector,
              List(BusinessEntity(eori, leadEORI = true))
            )
            undertakingCreated <- createUndertakingAndSendEmail(undertaking, updatedJourney).toContext
          } yield undertakingCreated
          result.fold(handleMissingSessionData("Undertaking create journey"))(identity)
        }
      )
  }

  private def createUndertakingAndSendEmail(
    undertaking: UndertakingCreate,
    undertakingJourney: UndertakingJourney
  )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] =
    for {
      ref <- escService.createUndertaking(undertaking)
      _ <- emailService.sendEmail(eori, EmailTemplate.CreateUndertaking, undertaking.toUndertakingWithRef(ref))
      auditEventCreateUndertaking = AuditEvent.CreateUndertaking(
        request.authorityId,
        ref,
        undertaking,
        timeProvider.now
      )
      _ = {
        undertakingJourney.addBusiness.value.getOrElse(List()).map { beEori =>
          // ETMP has race condition where concurrent adds will overwrite eachother :shrug:
          Await.result(escService.addMember(ref, BusinessEntity(beEori, false)), Duration(15, TimeUnit.SECONDS))
        }
      }
      _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
    } yield Redirect(routes.UndertakingController.getConfirmation(ref))

  def getConfirmation(ref: String): Action[AnyContent] = verifiedEmail.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber
      Ok(confirmationPage(UndertakingRef(ref), eori)).toFuture
  }

  def postConfirmation: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("Unexpected form submission"),
        form =>
          store
            .update[UndertakingJourney](_.setUndertakingConfirmation(form.value.toBoolean))
            .map { _ =>
              Redirect(routes.AccountController.getAccountPage())
            }
      )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      store.get[UndertakingJourney].flatMap {
        ensureUndertakingJourneyPresent(_) { journey =>
          for {
            updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          } yield Ok(
            amendUndertakingPage(
              updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
              routes.AccountController.getAccountPage().url
            )
          )

        }
      }
    }
  }

  private def updateIsAmendState(value: Boolean)(implicit e: EORI): Future[UndertakingJourney] =
    store.update[UndertakingJourney](_.copy(isAmend = value))

  def postAmendUndertaking: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      amendUndertakingForm
        .bindFromRequest()
        .fold(
          _ => throw new IllegalStateException("Unexpected form submission"),
          _ => {
            val result = for {
              updatedJourney <- updateIsAmendState(value = false).toContext
              undertakingName <- updatedJourney.about.value.toContext
              undertakingSector <- updatedJourney.sector.value.toContext
              retrievedUndertaking <- escService.retrieveUndertaking(eori).toContext
              undertakingRef <- retrievedUndertaking.reference.toContext
              updatedUndertaking = retrievedUndertaking
                .copy(name = UndertakingName(undertakingName), industrySector = undertakingSector)
              _ <- escService.updateUndertaking(updatedUndertaking).toContext
              _ = auditService.sendEvent(
                UndertakingUpdated(
                  request.authorityId,
                  eori,
                  undertakingRef,
                  updatedUndertaking.name,
                  updatedUndertaking.industrySector
                )
              )
            } yield Redirect(routes.AccountController.getAccountPage())
            result.getOrElse(handleMissingSessionData("Undertaking Journey"))
          }
        )
    }
  }

  def getDisableUndertakingWarning: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking(_ => Ok(disableUndertakingWarningPage()).toFuture)
  }

  def getDisableUndertakingConfirm: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking(_ =>
      Ok(disableUndertakingConfirmPage(disableUndertakingConfirmForm)).toFuture
    )
  }

  def postDisableUndertakingConfirm: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      disableUndertakingConfirmForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(disableUndertakingConfirmPage(errors)).toFuture,
          form => handleDisableUndertakingFormSubmission(form, undertaking)
        )
    }
  }

  def getUndertakingDisabled: Action[AnyContent] = verifiedEmail.async { implicit request =>
    Ok(undertakingDisabledPage()).withNewSession.toFuture
  }

  private def resetAllJourneys(implicit eori: EORI) =
    for {
      _ <- store.delete[EligibilityJourney]
      _ <- store.delete[UndertakingJourney]
      _ <- store.delete[NewLeadJourney]
      _ <- store.delete[NilReturnJourney]
      _ <- store.delete[BusinessEntityJourney]
      _ <- store.delete[BecomeLeadJourney]
      _ <- store.delete[SubsidyJourney]
    } yield ()

  private def handleDisableUndertakingFormSubmission(form: FormValues, undertaking: Undertaking)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedEnrolledRequest[_]
  ): Future[Result] =
    if (form.value.isTrue) {
      for {
        _ <- escService.disableUndertaking(undertaking)
        _ <- undertaking.undertakingBusinessEntity.traverse(be => resetAllJourneys(be.businessEntityIdentifier))
        _ = auditService.sendEvent[UndertakingDisabled](
          UndertakingDisabled(request.authorityId, undertaking.reference, timeProvider.today)
        )
        formattedDate = DateFormatter.govDisplayFormat(timeProvider.today)
        _ <- emailService.sendEmail(
          request.eoriNumber,
          DisableUndertakingToLead,
          undertaking,
          formattedDate
        )
        _ <- undertaking.undertakingBusinessEntity.filterNot(_.leadEORI).traverse { _ =>
          emailService.sendEmail(
            request.eoriNumber,
            DisableUndertakingToBusinessEntity,
            undertaking,
            formattedDate
          )
        }
      } yield Redirect(routes.UndertakingController.getUndertakingDisabled())
    }
    else Redirect(routes.AccountController.getAccountPage()).toFuture

  private def ensureUndertakingJourneyPresent(j: Option[UndertakingJourney])(f: UndertakingJourney => Future[Result]) =
    j.fold(Redirect(routes.UndertakingController.getAboutUndertaking()).toFuture)(f)

  private val aboutUndertakingForm: Form[FormValues] = Form(
    mapping("continue" -> mandatory("continue"))(FormValues.apply)(FormValues.unapply)
  )

  private val undertakingSectorForm: Form[FormValues] = formWithSingleMandatoryField("undertakingSector")
  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")
  private val confirmationForm: Form[FormValues] = formWithSingleMandatoryField("confirm")
  private val amendUndertakingForm: Form[FormValues] = formWithSingleMandatoryField("amendUndertaking")
  private val disableUndertakingConfirmForm: Form[FormValues] = formWithSingleMandatoryField("disableUndertakingConfirm")

  private val optionalEmailForm: Form[OptionalEmailFormInput] = Form(
    mapping(
      "using-stored-email" -> mandatory("using-stored-email"),
      "email" -> mandatoryIfEqual("using-stored-email", "false", email)
    )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)
  )

  private val emailForm: Form[FormValues] = Form(
    mapping(
      "email" -> email
    )(FormValues.apply)(FormValues.unapply)
  )

  private val addBusinessForm: Form[FormValues] = Form(
    mapping("addBusiness" -> mandatory("addBusiness"))(FormValues.apply)(FormValues.unapply)
  )

  def isEoriPrefixGB(eoriEntered: String) = eoriEntered.startsWith(eoriPrefix)

  def getValidEori(eoriEntered: String) =
    if (isEoriPrefixGB(eoriEntered)) eoriEntered else s"$eoriPrefix$eoriEntered"


  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (getValidEori(eori).length === 14 || getValidEori(eori).length === 17) Valid
    else Invalid("businessEntityEori.error.incorrect-length")
  }

  private val isEoriValid = Constraint[String] { eori: String =>
    if (getValidEori(eori).matches(EORI.regex)) Valid
    else Invalid("businessEntityEori.regex.error")
  }

  private val eoriForm: Form[FormValues] = Form(
    mapping(
      "businessEntityEori" -> mandatory("businessEntityEori")
        .verifying(isEoriLengthValid)
        .verifying(isEoriValid)
    )(eoriEntered => FormValues(getValidEori(eoriEntered)))(eori => eori.value.drop(2).some)
  )
  val eoriPrefix = "GB"

  private val removeBusinessForm = formWithSingleMandatoryField("removeBusiness")
}

