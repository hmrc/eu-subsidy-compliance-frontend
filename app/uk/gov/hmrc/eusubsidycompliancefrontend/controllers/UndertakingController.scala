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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailTemplate, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  override val store: Store,
  override val escService: EscService,
  emailService: EmailService,
  emailVerificationService: EmailVerificationService,
  timeProvider: TimeProvider,
  auditService: AuditService,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  confirmEmailPage: ConfirmEmailPage,
  inputEmailPage: InputEmailPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  confirmationPage: ConfirmationPage,
  amendUndertakingPage: AmendUndertakingPage,
  disableUndertakingWarningPage: DisableUndertakingWarningPage,
  disableUndertakingConfirmPage: DisableUndertakingConfirmPage,
  undertakingDisabledPage: UndertakingDisabledPage,
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with FormHelpers {

  import escCDSActionBuilder._
  def firstEmptyPage: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).map { journey =>
      journey.firstEmpty
        .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))(identity)
    }
  }

  def getUndertakingName: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val form = journey.name.value.fold(undertakingNameForm)(name => undertakingNameForm.fill(FormValues(name)))
      Ok(undertakingNamePage(form, journey.previous)).toFuture
    }
  }

  def postUndertakingName: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        undertakingNameForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(undertakingNamePage(errors, journey.previous)).toFuture,
            success = form => {
              for {
                updatedUndertakingJourney <- store.update[UndertakingJourney](_.setUndertakingName(form.value))
                redirect <- updatedUndertakingJourney.next
              } yield redirect
            }
          )
      case None => handleMissingSessionData("Undertaking Journey")

    }
  }

  def getSector: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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
              journey.name.value.getOrElse("")
            )
          ).toFuture
        }
      }
    }
  }

  def postSector: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors => {
            val result = for {
              undertakingName <- journey.name.value.toContext
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

  def getConfirmEmail: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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

  def postConfirmEmail: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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
            errors => BadRequest(confirmEmailPage(errors, EmailAddress(email), routes.EligibilityController.getCustomsWaivers().url)).toFuture,
            {
              case OptionalEmailFormInput("true", None) =>
                for {
                  _ <- emailVerificationService.verifyEori(request.eoriNumber)
                  _ <- store.update[UndertakingJourney](_.setVerifiedEmail(email))
                } yield Redirect(routes.UndertakingController.getCheckAnswers())
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
        errors => BadRequest(inputEmailPage(errors, routes.EligibilityController.getCustomsWaivers().url)).toFuture,
        form => {
          for {
            verificationId <- emailVerificationService.addVerificationRequest(request.eoriNumber, form.value)
            verificationResponse <- emailVerificationService.verifyEmail(request.authorityId, form.value, verificationId)
          } yield emailVerificationService.emailVerificationRedirect(verificationResponse)
        }
      )
    }
  }

  def getVerifyEmail(verificationId: String): Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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
          } else Future(Redirect(routes.UndertakingController.getUndertakingName().url))
        } yield redirect
      case None => handleMissingSessionData("Undertaking Journey")
    }
  }

  def getCheckAnswers: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          undertakingName <- journey.name.value.toContext
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- journey.verifiedEmail.value.toContext
        } yield Ok(cyaPage(UndertakingName(undertakingName), eori, undertakingSector, undertakingVerifiedEmail, journey.previous))
        result.fold(Redirect(journey.previous))(identity)
      case _ => Redirect(routes.UndertakingController.getUndertakingName()).toFuture
    }
  }

  def postCheckAnswers: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form => {
          val result = for {
            updatedJourney <- store.update[UndertakingJourney](_.setUndertakingCYA(form.value.toBoolean)).toContext
            undertakingName <- updatedJourney.name.value.toContext
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
  )(implicit request: AuthenticatedEscRequest[_], eori: EORI): Future[Result] =
    for {
      ref <- escService.createUndertaking(undertaking)
      _ <- emailService.sendEmail(eori, EmailTemplate.CreateUndertaking, undertaking.toUndertakingWithRef(ref))
      auditEventCreateUndertaking = AuditEvent.CreateUndertaking(
        request.authorityId,
        ref,
        undertaking,
        timeProvider.now
      )
      _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
    } yield Redirect(routes.UndertakingController.getConfirmation(ref, undertakingJourney.name.value.getOrElse("")))

  def getConfirmation(ref: String, name: String): Action[AnyContent] = withCDSAuthenticatedUser.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber
      Ok(confirmationPage(UndertakingRef(ref), UndertakingName(name), eori)).toFuture
  }

  def postConfirmation: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store
            .update[UndertakingJourney](_.setUndertakingConfirmation(form.value.toBoolean))
            .map { _ =>
              Redirect(routes.BusinessEntityController.getAddBusinessEntity())
            }
      )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      store.get[UndertakingJourney].flatMap {
        ensureUndertakingJourneyPresent(_) { journey =>
          for {
            updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          } yield Ok(
            amendUndertakingPage(
              updatedJourney.name.value.fold(handleMissingSessionData("Undertaking Name"))(UndertakingName(_)),
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

  def postAmendUndertaking: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      amendUndertakingForm
        .bindFromRequest()
        .fold(
          _ => throw new IllegalStateException("value hard-coded, form hacking?"),
          _ => {
            val result = for {
              updatedJourney <- updateIsAmendState(value = false).toContext
              undertakingName <- updatedJourney.name.value.toContext
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

  def getDisableUndertakingWarning: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking(undertaking => Ok(disableUndertakingWarningPage(undertaking.name)).toFuture)
  }

  def getDisableUndertakingConfirm: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking(undertaking =>
      Ok(disableUndertakingConfirmPage(disableUndertakingConfirmForm, undertaking.name)).toFuture
    )
  }

  def postDisableUndertakingConfirm: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      disableUndertakingConfirmForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(disableUndertakingConfirmPage(errors, undertaking.name)).toFuture,
          form => handleDisableUndertakingFormSubmission(form, undertaking)
        )
    }
  }

  def getUndertakingDisabled: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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
    request: AuthenticatedEscRequest[_]
  ): Future[Result] =
    if (form.value == "true") {
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
    j.fold(Redirect(routes.UndertakingController.getUndertakingName()).toFuture)(f)

  private val undertakingNameForm: Form[FormValues] = Form(
    mapping("undertakingName" -> mandatory("undertakingName"))(FormValues.apply)(FormValues.unapply).verifying(
      "undertakingName.regex.error",
      fields =>
        fields match {
          case a if a.value.matches(UndertakingName.regex) => true
          case _ => false
        }
    )
  )

  private val undertakingSectorForm: Form[FormValues] = Form(
    mapping("undertakingSector" -> mandatory("undertakingSector"))(FormValues.apply)(FormValues.unapply)
  )

  private val cyaForm: Form[FormValues] = Form(mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

  private val confirmationForm: Form[FormValues] = Form(
    mapping("confirm" -> mandatory("confirm"))(FormValues.apply)(FormValues.unapply)
  )

  private val amendUndertakingForm: Form[FormValues] = Form(
    mapping("amendUndertaking" -> mandatory("amendUndertaking"))(FormValues.apply)(FormValues.unapply)
  )

  private val disableUndertakingConfirmForm: Form[FormValues] = Form(
    mapping("disableUndertakingConfirm" -> mandatory("disableUndertakingConfirm"))(FormValues.apply)(FormValues.unapply)
  )

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

}
