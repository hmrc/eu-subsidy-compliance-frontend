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
import play.api.i18n.MessagesApi
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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EmailStatus, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{Store, UndertakingCache}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.{HeaderCarrier, HttpVerbs, NotFoundException}
import play.api.mvc.Call
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms.UndertakingCyaFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.NaceLevel4Catalogue

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
  undertakingDisabledPage: UndertakingDisabledPage,
  undertakingCache: UndertakingCache,
  navigator: Navigator
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with EmailVerificationSupport
    with ControllerFormHelpers {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

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

  private def generateBackLink(emailStatus: EmailStatus): Call = {
    emailStatus match {
      case EmailStatus.Unverified => routes.UnverifiedEmailController.unverifiedEmail
      case EmailStatus.Amend => routes.UndertakingController.getAmendUndertakingDetails
      case EmailStatus.BecomeLead => routes.BecomeLeadController.getConfirmEmail
      case EmailStatus.CYA => routes.UndertakingController.getCheckAnswers
      case _ => routes.UndertakingController.getSector
    }
  }

  def firstEmptyPage: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info(s"UndertakingController.firstEmptyPage attempting to find the journeys next step for EORI:$eori")
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).map { journey =>
      logger.info(s"UndertakingJourney for EORI:$eori is completed ${journey.isComplete}")
      logger.info(s"UndertakingJourney's firstEmpty for EORI:$eori is ${journey.firstEmpty}")
      journey.steps.foreach { step =>
        logger.info(s"UndertakingJourney step for EORI:$eori ${step.uri}=${step.value.isDefined}")
      }

      journey.firstEmpty
        .fold(Redirect(routes.AddBusinessEntityController.getAddBusinessEntity()))(identity)
    }
  }

  def getAboutUndertaking: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val form = journey.about.value.fold(aboutUndertakingForm)(name => aboutUndertakingForm.fill(FormValues(name)))
      store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
      Ok(aboutUndertakingPage(form, journey.previous)).toFuture
    }
  }

  def postAboutUndertaking: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
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
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      if (journey.mode.equals(appConfig.UpdateNaceMode)) {

        val sector = journey.sector.value match {
          case Some(value) =>
            if (value.toString.length < 2 && value.toString.equals("0")) value.toString
            else if (!value.toString.take(2).equals("01") && !value.toString.take(2).equals("03")) "00"
            else value.toString.take(2)
          case None => ""
        }

        Ok(
          undertakingSectorPage(
            undertakingSectorForm.fill(FormValues(sector)),
            journey.previous,
            journey.about.value.getOrElse(""),
            journey.mode
          )
        ).toFuture
      } else {
        getSectorPage()
      }
    }
  }

  private def getSectorPage()(implicit request: AuthenticatedEnrolledRequest[_]) = {
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      runStepIfEligible(journey) {
        val sector = journey.sector.value match {
          case Some(value) =>
            if (value.toString.length < 2 && value.toString.equals("0")) value.toString
            else if (!value.toString.take(2).equals("01") && !value.toString.take(2).equals("03")) "00"
            else value.toString.take(2)
          case None => ""
        }

        Ok(
          undertakingSectorPage(
            undertakingSectorForm.fill(FormValues(sector)),
            journey.previous,
            journey.about.value.getOrElse(""),
            journey.mode
          )
        ).toFuture
      }
    }
  }

  def getSectorForUpdate: Action[AnyContent] = enrolled.async { implicit request =>
    getSectorPage()
  }

  def postSector: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    updateSector()
  }
  def updateIndustrySector: Action[AnyContent] = enrolled.async { implicit request =>
    updateSector()
  }

  private def updateSector()(implicit request: AuthenticatedEnrolledRequest[_]) = {
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(
              undertakingSectorPage(errors, journey.previous, journey.about.value.get, journey.mode)
            ).toContext,
          form => {
            val previousAnswer = journey.sector.value match {
              case Some(value) =>
                if (value.toString.length < 2) value.toString
                else if (!value.toString.take(2).equals("01") && !value.toString.take(2).equals("03")) "00"
                else value.toString.take(2)
              case None => ""
            }

            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toContext
            else if (previousAnswer.equals(form.value))
                Redirect(navigator.nextPage(form.value, journey.mode)).toContext
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                  .toContext
                updatedStoreFlag <- store.update[UndertakingJourney](_.copy(isNaceCYA = false)).toContext
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        )
    }
  }

  def getConfirmEmail: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.UndertakingController.getAboutUndertaking).toFuture) { journey =>
        handleConfirmEmailGet[UndertakingJourney](
          previous = Call(HttpVerbs.GET, journey.previous),
          formAction = routes.UndertakingController.postConfirmEmail
        )
      }
  }

  def getAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = generateBackLink(status).url
      Future.successful(Ok(inputEmailPage(emailForm, backLink, Some(status))))
  }

  def postAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = generateBackLink(status)

      val nextUrl = (id: String) => routes.UndertakingController.getVerifyEmail(id, Some(status)).url
      val reEnterEmailUrl = routes.UndertakingController.getAddEmailForVerification(status).url

      emailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(inputEmailPage(errors, backLink.url, Some(status))).toFuture,
          form =>
            emailVerificationService.makeVerificationRequestAndRedirect(
              email = form.value,
              previousPage = backLink.url,
              nextPageUrl = nextUrl,
              reEnterEmailUrl = reEnterEmailUrl
            )
        )
  }

  override def addVerifiedEmailToJourney(implicit eori: EORI): Future[Unit] =
    store
      .update[UndertakingJourney](_.setHasVerifiedEmail(true))
      .void

  def postConfirmEmail: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      handleConfirmEmailPost[UndertakingJourney](
        previous = journey.previous,
        inputEmailRoute = routes.UndertakingController.getAddEmailForVerification(EmailStatus.New).url,
        next = {
          val call =
            if (journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails
            else routes.UndertakingController.getAddBusiness

          if (journey.cya.value.getOrElse(false)) routes.UndertakingController.getCheckAnswers.url
          else call.url
        },
        formAction = routes.UndertakingController.postConfirmEmail
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
        case Some(emailStatus @ EmailStatus.Amend) =>
          (
            routes.UndertakingController.getAmendUndertakingDetails,
            routes.UndertakingController.getAmendUndertakingDetails
          )
        case Some(emailStatus @ EmailStatus.BecomeLead) =>
          (
            routes.UndertakingController.getAddEmailForVerification(emailStatus),
            routes.BecomeLeadController.getBecomeLeadEori()
          )
        case Some(emailStatus @ EmailStatus.CYA) =>
          (
            routes.UndertakingController.getAddEmailForVerification(emailStatus),
            routes.UndertakingController.getCheckAnswers
          )
        case _ =>
          (routes.UndertakingController.getConfirmEmail, routes.UndertakingController.getAddBusiness)
      }

      handleVerifyEmailGet[UndertakingJourney](
        verificationId = verificationId,
        previous = previousAndNext._1,
        next = previousAndNext._2
      )
    }
  }

  def getAddBusiness: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
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

  def postAddBusiness: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
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

  def backFromCheckYourAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[UndertakingJourney](_.setUndertakingCYA(false))
      .map { _ =>
        Redirect(routes.UndertakingController.getAddBusiness)
      }
  }

  def getCheckAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.UndertakingController.getAboutUndertaking).toFuture) { journey =>
        val result = for {
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- emailService.retrieveVerifiedEmailAddressByEORI(eori).toContext
          undertakingAddBusiness <- journey.addBusiness.value.toContext
          _ <- store.update[UndertakingJourney](j => j.copy(cya = UndertakingCyaFormPage(Some(true)))).toContext
        } yield {
          val naceLevel3Code = undertakingSector.toString.dropRight(1)
          val naceLevel2Code = undertakingSector.toString.take(2)
          val naceLevel1Code: String = naceLevel2Code match {
            case "01" | "02" | "03" => "A"
            case "05" | "06" | "07" | "08" | "09" => "B"
            case "35" => "D"
            case "36" | "37" | "38" | "39" => "E"
            case "41" | "42" | "43" => "F"
            case "46" | "47" => "G"
            case "49" | "50" | "51" | "52" | "53" => "H"
            case "55" | "56" => "I"
            case "58" | "59" | "60" => "J"
            case "61" | "62" | "63" => "K"
            case "64" | "65" | "66" => "L"
            case "68" => "M"
            case "69" | "70" | "71" | "72" | "73" | "74" | "75" => "N"
            case "77" | "78" | "79" | "80" | "81" | "82" => "O"
            case "84" => "P"
            case "85" => "Q"
            case "86" | "87" | "88" => "R"
            case "90" | "91" | "92" | "93" => "S"
            case "94" | "95" | "96" => "T"
            case "97" | "98" => "U"
            case "99" => "V"
            case _ => "C"
          }
          val industrySectorKey: String = naceLevel2Code match {
            case "01" => "agriculture"
            case "03" => "fisheryAndAquaculture"
            case _ => "generalTrade"
          }
          Ok(
            cyaPage(
              eori = eori,
              sector = undertakingSector,
              verifiedEmail = undertakingVerifiedEmail,
              addBusiness = undertakingAddBusiness.toString,
              naceLevel1Code = naceLevel1Code,
              naceLevel2Code = naceLevel2Code,
              naceLevel3Code = naceLevel3Code,
              industrySectorKey = industrySectorKey,
              previous = routes.UndertakingController.backFromCheckYourAnswers.url
            )
          )
        }

        result.getOrElse(Redirect(journey.previous))
      }
  }

  def postCheckAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
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
            _ <- store.update[UndertakingJourney](_.setSubmitted(true)).toContext
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
        newlyCreatedUndertaking <- escService.retrieveUndertaking(eori).map {
          case Some(ut) => ut
          case None =>
            logger.error(s"Unable to fetch undertaking with reference: $ref")
            throw new NotFoundException(s"Unable to fetch undertaking with reference: $ref")
        }
        _ <- emailService.sendEmail(eori, EmailTemplate.CreateUndertaking, newlyCreatedUndertaking)
        _ <- undertakingCache.put[Undertaking](eori, newlyCreatedUndertaking)
        auditEventCreateUndertaking = AuditEvent.CreateUndertaking(
          request.authorityId,
          ref,
          undertaking,
          sectorCap = newlyCreatedUndertaking.industrySectorLimit,
          timeProvider.now
        )
        _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
      } yield Redirect(routes.UndertakingController.getConfirmation(ref))
    ).recoverWith { case error: NotFoundException =>
      logger.error(s"Error creating undertaking: $error")
      errorHandler.internalServerErrorTemplate(request).map { html =>
        InternalServerError(html)
      }
    }

  def getConfirmation(ref: String): Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def postConfirmation: Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def getAmendUndertakingDetails: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
        for {
          updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          verifiedEmail <- emailService.retrieveVerifiedEmailAddressByEORI(eori)
        } yield {
          val naceLevel4Code = updatedJourney.sector.value.toString.dropRight(1).drop(5)
          val naceLevel3Code = updatedJourney.sector.value.toString.dropRight(2).drop(5)
          val naceLevel2Code = naceLevel3Code.take(2)
          val naceLevel1Code: String = naceLevel2Code match {
            case "01" | "02" | "03" => "A"
            case "05" | "06" | "07" | "08" | "09" => "B"
            case "35" => "D"
            case "36" | "37" | "38" | "39" => "E"
            case "41" | "42" | "43" => "F"
            case "46" | "47" => "G"
            case "49" | "50" | "51" | "52" | "53" => "H"
            case "55" | "56" => "I"
            case "58" | "59" | "60" => "J"
            case "61" | "62" | "63" => "K"
            case "64" | "65" | "66" => "L"
            case "68" => "M"
            case "69" | "70" | "71" | "72" | "73" | "74" | "75" => "N"
            case "77" | "78" | "79" | "80" | "81" | "82" => "O"
            case "84" => "P"
            case "85" => "Q"
            case "86" | "87" | "88" => "R"
            case "90" | "91" | "92" | "93" => "S"
            case "94" | "95" | "96" => "T"
            case "97" | "98" => "U"
            case "99" => "V"
            case _ => "C"
          }
          val industrySectorKey: String = naceLevel2Code match {
            case "01" => "agriculture"
            case "03" => "fisheryAndAquaculture"
            case _ => "generalTrade"
          }
          val naceLevel4Notes = NaceLevel4Catalogue
            .fromMessages(naceLevel4Code)
            .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceLevel4Code"))

          Ok(
            amendUndertakingPage(
              updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
              verifiedEmail,
              naceLevel1Code = naceLevel1Code,
              naceLevel2Code = naceLevel2Code,
              naceLevel3Code = naceLevel3Code,
              naceLevel4Notes = naceLevel4Notes,
              industrySectorKey = industrySectorKey,
              previous = routes.AccountController.getAccountPage.url
            )
          )
        }
      }
    }
  }

  private def updateIsAmendState(value: Boolean)(implicit e: EORI): Future[UndertakingJourney] =
    store.update[UndertakingJourney](_.copy(isAmend = value))

  def postAmendUndertaking: Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def getDisableUndertakingWarning: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking(_ => Ok(disableUndertakingWarningPage()).toFuture)
  }

  def getDisableUndertakingConfirm: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking(_ => Ok(disableUndertakingConfirmPage(disableUndertakingConfirmForm)).toFuture)
  }

  def postDisableUndertakingConfirm: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { undertaking =>
      disableUndertakingConfirmForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(disableUndertakingConfirmPage(errors)).toFuture,
          form => handleDisableUndertakingFormSubmission(form, undertaking)
        )
    }
  }

  def getUndertakingDisabled: Action[AnyContent] = verifiedEori.async { implicit request =>
    Ok(undertakingDisabledPage()).withNewSession.toFuture
  }

  private def handleDisableUndertakingFormSubmission(form: FormValues, undertaking: Undertaking)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedEnrolledRequest[_]
  ): Future[Result] =
    if (form.value.isTrue) {
      logger.info("UndertakingController.handleDisableUndertakingFormSubmission")

      for {
        _ <- escService.disableUndertaking(undertaking)
        _ <- undertaking.undertakingBusinessEntity.traverse(be => store.deleteAll(be.businessEntityIdentifier))
        _ = auditService.sendEvent[UndertakingDisabled](
          UndertakingDisabled(request.authorityId, undertaking.reference, timeProvider.today, undertaking)
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
          "UndertakingController.handleDisableUndertakingFormSubmission - form.value.isTrue so redirecting to routes.UndertakingController.getUndertakingDisabled"
        )
        Redirect(routes.UndertakingController.getUndertakingDisabled)
      }
    } else {
      logger.info(
        "UndertakingController.handleDisableUndertakingFormSubmission - form.value.isTrue is not true so redirecting to routes.AccountController.getAccountPage"
      )
      Redirect(routes.AccountController.getAccountPage).toFuture
    }

}
