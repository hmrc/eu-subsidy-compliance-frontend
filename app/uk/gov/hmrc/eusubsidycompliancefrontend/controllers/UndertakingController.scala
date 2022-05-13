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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  store: Store,
  override val escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  sendEmailHelperService: EmailService,
  timeProvider: TimeProvider,
  auditService: AuditService,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  confirmationPage: ConfirmationPage,
  amendUndertakingPage: AmendUndertakingPage,
  disableUndertakingWarningPage: DisableUndertakingWarningPage,
  disableUndertakingConfirmPage: DisableUndertakingConfirmPage,
  undertakingDisabledPage: UndertakingDisabledPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import escCDSActionBuilder._
  val CreateUndertaking = "createUndertaking"

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
    journeyTraverseService.getPrevious[UndertakingJourney].flatMap { previous =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors => {
            val result: OptionT[Future, Result] = for {
              undertakingJourney <- store.get[UndertakingJourney].toContext
              undertakingName <- undertakingJourney.name.value.toContext
            } yield BadRequest(undertakingSectorPage(errors, previous, undertakingName))
            result.fold(handleMissingSessionData("Undertaking Journey"))(identity)
          },
          form =>
            for {
              updatedUndertakingJourney <- store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
              redirect <- updatedUndertakingJourney.next
            } yield redirect
        )
    }
  }

  def getCheckAnswers: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          undertakingName <- journey.name.value.toContext
          undertakingSector <- journey.sector.value.toContext
        } yield Ok(cyaPage(UndertakingName(undertakingName), eori, undertakingSector, journey.previous))
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
            undertaking = Undertaking(
              None,
              name = UndertakingName(undertakingName),
              industrySector = undertakingSector,
              None,
              None,
              List(BusinessEntity(eori, leadEORI = true))
            )
            undertakingCreated <- createUndertakingAndSendEmail(undertaking, updatedJourney).toContext
          } yield undertakingCreated
          result.fold(handleMissingSessionData("Undertaking create journey"))(identity)
        }
      )
  }

  private def createUndertakingAndSendEmail(
    undertaking: Undertaking,
    undertakingJourney: UndertakingJourney
  )(implicit request: AuthenticatedEscRequest[_], eori: EORI): Future[Result] =
    for {
      ref <- escService.createUndertaking(undertaking)
      _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
        eori,
        None,
        CreateUndertaking,
        undertaking,
        ref,
        None
      )
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
    withLeadUndertaking(undertaking => Ok(disableUndertakingWarningPage(undertaking.name.toString)).toFuture)
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
          form => handleFormSubmission(form, undertaking)
        )
    }
  }

  def getUndertakingDisabled: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    Ok(undertakingDisabledPage()).toFuture
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

  private def handleFormSubmission(form: FormValues, undertaking: Undertaking)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    if (form.value == "true") {
      for {
        _ <- escService.disableUndertaking(undertaking)
        _ <- undertaking.undertakingBusinessEntity.traverse(be => resetAllJourneys(be.businessEntityIdentifier))
      } yield Redirect(routes.UndertakingController.getUndertakingDisabled())
    } else Redirect(routes.AccountController.getAccountPage()).toFuture

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

}
