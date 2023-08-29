/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField, mandatory}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EmailStatus, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

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
  val errorHandler: ErrorHandler,
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
  undertakingDisabledPage: UndertakingDisabledPage
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with EmailVerificationSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val aboutUndertakingForm: Form[FormValues] = Form(
    mapping("continue" -> mandatory("continue"))(FormValues.apply)(FormValues.unapply)
  )

  private val undertakingSectorForm: Form[FormValues] = formWithSingleMandatoryField("undertakingSector")
  private val addBusinessForm: Form[FormValues] = formWithSingleMandatoryField("addBusinessIntent")
  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")
  private val confirmationForm: Form[FormValues] = formWithSingleMandatoryField("confirm")
  private val amendUndertakingForm: Form[FormValues] = formWithSingleMandatoryField("amendUndertaking")
  private val disableUndertakingConfirmForm: Form[FormValues] = formWithSingleMandatoryField(
    "disableUndertakingConfirm"
  )

  def firstEmptyPage: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info(s"UndertakingController.firstEmptyPage attempting to find the journeys next step for EORI:$eori")
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).map { journey =>
      logger.info(s"UndertakingJourney for EORI:$eori is completed ${journey.isComplete}")
      logger.info(s"UndertakingJourney's firstEmpty for EORI:$eori is ${journey.firstEmpty}")
      journey.steps.foreach { step =>
        logger.info(s"UndertakingJourney step for EORI:$eori" + step.uri + "=" + step.value.isDefined)
      }

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
            _ =>
              for {
                // We store the EORI in the undertaking name field.
                updatedUndertakingJourney <- store.update[UndertakingJourney](_.setUndertakingName(eori))
                redirect <- updatedUndertakingJourney.next
              } yield redirect
          )
      case None => handleMissingSessionData("Undertaking Journey")

    }
  }

  def getSector: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      runStepIfEligible(journey) {
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
    }
  }

  def postSector: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    processFormSubmission[UndertakingJourney] { journey =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(undertakingSectorPage(errors, journey.previous, journey.about.value.get)).toContext,
          form =>
            store
              .update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
              .flatMap(_.next)
              .toContext
        )
    }
  }

  def getConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    handleConfirmEmailGet[UndertakingJourney](
      previous = routes.UndertakingController.getSector,
      formAction = routes.UndertakingController.postConfirmEmail
    )
  }

  def getAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = status match {
        case EmailStatus.Unverified => routes.UnverifiedEmailController.unverifiedEmail.url
        case _ => routes.UndertakingController.getSector.url
      }
      Future.successful(Ok(inputEmailPage(emailForm, backLink, Some(status))))
  }

  def postAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = status match {
        case EmailStatus.Unverified => routes.UnverifiedEmailController.unverifiedEmail
        case _ => routes.UndertakingController.getSector
      }

      val nextUrl = (id: String) => routes.UndertakingController.getVerifyEmail(id, Some(status)).url
      val reEnterEmailUrl = routes.UndertakingController.getAddEmailForVerification(status).url

      emailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(inputEmailPage(errors, backLink.url, Some(status))).toFuture,
          form =>
            emailVerificationService.makeVerificationRequestAndRedirect(
              email = form.value,
              previousPage = backLink,
              nextPageUrl = nextUrl,
              reEnterEmailUrl = reEnterEmailUrl
            )
        )
  }

  override def addVerifiedEmailToJourney(email: String)(implicit eori: EORI): Future[Unit] =
    store
      .update[UndertakingJourney](_.setVerifiedEmail(email))
      .map(_ => ())

  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      handleConfirmEmailPost[UndertakingJourney](
        previous =
          if (journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails
          else routes.UndertakingController.getConfirmEmail,
        next =
          if (journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails
          else routes.UndertakingController.getAddBusiness,
        formAction = routes.UndertakingController.postConfirmEmail,
        generateVerifyEmailUrl = (id: String) => routes.UndertakingController.getVerifyEmail(id).url
      )
    }
  }

  def getVerifyEmail(
    verificationId: String,
    status: Option[EmailStatus] = Some(EmailStatus.New)
  ): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      val previousAndNext = status match {
        case Some(emailStatus @ EmailStatus.Unverified) =>
          (
            routes.UndertakingController.getAddEmailForVerification(emailStatus),
            routes.AccountController.getAccountPage
          )
        case _ if journey.isAmend =>
          (
            routes.UndertakingController.getAmendUndertakingDetails,
            routes.UndertakingController.getAmendUndertakingDetails
          )
        case _ => (routes.UndertakingController.getConfirmEmail, routes.UndertakingController.getAddBusiness)
      }

      handleVerifyEmailGet[UndertakingJourney](
        verificationId = verificationId,
        previous = previousAndNext._1,
        next = previousAndNext._2
      )
    }
  }

  def getAddBusiness: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      runStepIfEligible(journey) {
        val form = journey.addBusiness.value.fold(addBusinessForm)(addBusiness =>
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
            store
              .update[UndertakingJourney](_.setAddBusiness(form.value.isTrue))
              .flatMap(_.next)
              .toContext
        )
    }
  }

  def getCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.UndertakingController.getAboutUndertaking).toFuture) { journey =>
        val result = for {
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- journey.verifiedEmail.value.toContext
          undertakingAddBusiness <- journey.addBusiness.value.toContext
        } yield Ok(
          cyaPage(eori, undertakingSector, undertakingVerifiedEmail, undertakingAddBusiness.toString, journey.previous)
        )

        result.getOrElse(Redirect(journey.previous))
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
    undertaking: UndertakingCreate
  )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] =
    (
      for {
        ref <- escService.createUndertaking(undertaking)
        _ <- emailService.sendEmail(eori, EmailTemplate.CreateUndertaking, undertaking.toUndertakingWithRef(ref))
        newlyCreatedUndertaking <- escService.retrieveUndertaking(eori).map {
          case Some(ut) => ut
          case None => {
            logger.error(s"Unable to fetch undertaking with reference: $ref")
            throw new NotFoundException(s"Unable to fetch undertaking with reference: $ref")
          }
        }
        auditEventCreateUndertaking = AuditEvent.CreateUndertaking(
          request.authorityId,
          ref,
          undertaking,
          sectorCap = newlyCreatedUndertaking.industrySectorLimit.getOrElse(
            FinancialDashboardSummary.DefaultSectorLimits(undertaking.industrySector)
          ), //fixme update once industrySectorLimit has been made mandatory (ESC-1087)
          timeProvider.now
        )
        _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
      } yield Redirect(routes.UndertakingController.getConfirmation(ref))
    ).recover {
      case error: NotFoundException => {
        logger.error(s"Error creating undertaking: $error")
        InternalServerError(errorHandler.internalServerErrorTemplate)
      }
    }

  def getConfirmation(ref: String): Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          addBusiness <- journey.addBusiness.value.toContext
        } yield Ok(confirmationPage(UndertakingRef(ref), eori, addBusiness))
        result.fold(Redirect(journey.previous))(identity)
      case _ => Redirect(routes.UndertakingController.getAboutUndertaking).toFuture
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
              Redirect(routes.AccountController.getAccountPage)
            }
      )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
        for {
          updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          verifiedEmail <- emailVerificationService.getEmailVerification(eori)
        } yield Ok(
          amendUndertakingPage(
            updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
            verifiedEmail.fold(handleMissingSessionData("Verified email"))(e => e.email),
            routes.AccountController.getAccountPage.url
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
            } yield Redirect(routes.AccountController.getAccountPage)
            result.getOrElse(handleMissingSessionData("Undertaking Journey"))
          }
        )
    }
  }

  def getDisableUndertakingWarning: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking(_ => Ok(disableUndertakingWarningPage()).toFuture)
  }

  def getDisableUndertakingConfirm: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking(_ => Ok(disableUndertakingConfirmPage(disableUndertakingConfirmForm)).toFuture)
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

  private def handleDisableUndertakingFormSubmission(form: FormValues, undertaking: Undertaking)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedEnrolledRequest[_]
  ): Future[Result] =
    if (form.value.isTrue) {
      logger.info("SelectNewLeadController.handleDisableUndertakingFormSubmission")

      for {
        _ <- escService.disableUndertaking(undertaking)
        _ <- undertaking.undertakingBusinessEntity.traverse(be => store.deleteAll(be.businessEntityIdentifier))
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
      } yield {
        logger.info(
          "SelectNewLeadController.handleDisableUndertakingFormSubmission - form.value.isTrue so redirecting to routes.UndertakingController.getUndertakingDisabled"
        )
        Redirect(routes.UndertakingController.getUndertakingDisabled)
      }
    } else {
      logger.info(
        "SelectNewLeadController.handleDisableUndertakingFormSubmission - form.value.isTrue is not true so redirecting to routes.AccountController.getAccountPage"
      )
      Redirect(routes.AccountController.getAccountPage).toFuture
    }

}
