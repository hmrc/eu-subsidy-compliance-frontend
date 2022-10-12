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
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject() (
                                        mcc: MessagesControllerComponents,
                                        actionBuilders: ActionBuilders,
                                        override val store: Store,
                                        override val escService: EscService,
                                        override val emailService: EmailService,
                                        override val emailVerificationService: EmailVerificationService,
                                        timeProvider: TimeProvider,
                                        auditService: AuditService,
                                        aboutUndertakingPage: AboutUndertakingPage,
                                        undertakingSectorPage: UndertakingSectorPage,
                                        undertakingAddBusinessPage: UndertakingAddBusinessPage,
                                        override val confirmEmailPage: ConfirmEmailPage,
                                        override val inputEmailPage: InputEmailPage,
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
    with EmailVerificationSupport
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
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking()) { journey =>
      if (journey.isEligibleForStep) {
        val form = journey.sector.value.fold(undertakingSectorForm) { sector =>
          undertakingSectorForm.fill(FormValues(sector.id.toString))
        }

        Ok(
          undertakingSectorPage(
            form,
            journey.previous,
            journey.about.value.getOrElse("")
          )
        ).toFuture
      }
      else Redirect(journey.previous).toFuture
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
    handleConfirmEmailGet[UndertakingJourney](
      previous = routes.UndertakingController.getSector(),
      formAction = routes.UndertakingController.postConfirmEmail()
    )
  }

  override def addVerifiedEmailToJourney(email: String)(implicit eori: EORI): Future[Unit] = {
    store
      .update[UndertakingJourney](_.setVerifiedEmail(email))
      .map(_ => ())
  }

  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking()) { journey =>
      handleConfirmEmailPost[UndertakingJourney](
        previous = if(journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails() else routes.UndertakingController.getConfirmEmail(),
        next = if(journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails() else routes.UndertakingController.getAddBusiness(),
        formAction = routes.UndertakingController.postConfirmEmail(),
        generateVerifyEmailUrl = (id: String) => routes.UndertakingController.getVerifyEmail(id).url
      )
    }
  }

  def getVerifyEmail(verificationId: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking()) { journey =>
      handleVerifyEmailGet[UndertakingJourney](
        verificationId = verificationId,
        previous = if(journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails() else routes.UndertakingController.getConfirmEmail(),
        next = if(journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails() else routes.UndertakingController.getAddBusiness(),
      )
    }
  }

  def getAddBusiness: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking()) { journey =>
      if (!journey.isEligibleForStep) {
        Redirect(journey.previous).toFuture
      } else {
        val form = journey.addBusiness.value.fold(undertakingSectorForm)(addBusiness =>
          addBusinessForm.fill(FormValues(addBusiness.toString))
        )
        Ok(
          undertakingAddBusinessPage(
            form,
            journey.previous
          )
        ).toFuture
      }
    }
  }

  def postAddBusiness: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(undertakingAddBusinessPage(errors, journey.previous)).toContext,
          form =>
            store.update[UndertakingJourney](_.setAddBusiness(form.value.isTrue))
              .flatMap(_.next)
              .toContext
        )
    }
  }


  def getCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- journey.verifiedEmail.value.toContext
        } yield Ok(cyaPage(eori, undertakingSector, undertakingVerifiedEmail, journey.previous))
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
            undertakingCreated <- createUndertakingAndSendEmail(undertaking).toContext
          } yield undertakingCreated
          result.fold(handleMissingSessionData("Undertaking create journey"))(identity)
        }
      )
  }

  private def createUndertakingAndSendEmail(
    undertaking: UndertakingCreate,
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
      _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
    } yield Redirect(routes.UndertakingController.getConfirmation(ref))

  def getConfirmation(ref: String): Action[AnyContent] = verifiedEmail.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber

      store.get[UndertakingJourney].flatMap {
        case Some(journey) =>
          val result: OptionT[Future, Result] = for {
            addBusiness <- journey.addBusiness.value.toContext
          } yield Ok(confirmationPage(UndertakingRef(ref), eori, addBusiness))
          result.fold(Redirect(journey.previous))(identity)
        case _ => Redirect(routes.UndertakingController.getAboutUndertaking()).toFuture
      }
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

      withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking()) { journey =>
        for {
          updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          verifiedEmail <- emailVerificationService.getEmailVerification(eori)
        } yield Ok(
          amendUndertakingPage(
            updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
            verifiedEmail.fold(handleMissingSessionData("Verified email"))(e => e.email),
            routes.AccountController.getAccountPage().url
          )
        )
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

  private val aboutUndertakingForm: Form[FormValues] = Form(
    mapping("continue" -> mandatory("continue"))(FormValues.apply)(FormValues.unapply)
  )

  private val undertakingSectorForm: Form[FormValues] = formWithSingleMandatoryField("undertakingSector")
  private val addBusinessForm: Form[FormValues] = formWithSingleMandatoryField("addBusiness")
  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")
  private val confirmationForm: Form[FormValues] = formWithSingleMandatoryField("confirm")
  private val amendUndertakingForm: Form[FormValues] = formWithSingleMandatoryField("amendUndertaking")
  private val disableUndertakingConfirmForm: Form[FormValues] = formWithSingleMandatoryField("disableUndertakingConfirm")

}
