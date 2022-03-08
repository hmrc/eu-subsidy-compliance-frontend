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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.{FutureOptionToOptionTOps, FutureToOptionTOps, OptionToOptionTOps}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  sendEmailHelperService: SendEmailHelperService,
  timeProvider: TimeProvider,
  auditService: AuditService,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  confirmationPage: ConfirmationPage,
  amendUndertakingPage: AmendUndertakingPage
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._
  val CreateUndertaking = "createUndertaking"

  def firstEmptyPage: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].map {
      case Some(journey) =>
        journey.firstEmpty
          .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))(identity)
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def getUndertakingName: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val form = journey.name.value.fold(undertakingNameForm)(name => undertakingNameForm.fill(FormValues(name)))
        Ok(undertakingNamePage(form, journey.previous)).toFuture
      case None => // initialise the empty Journey model
        val journey = UndertakingJourney()
        store.put(journey).map { _ =>
          Ok(undertakingNamePage(undertakingNameForm, UndertakingJourney().previous))
        }
    }
  }

  def postUndertakingName: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        undertakingNameForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(undertakingNamePage(errors, journey.previous)).toFuture,
            success = form => {
              for {
                updatedUndertakingJourney <- store.update[UndertakingJourney](updateUndertakingName(form))
                redirect <- updatedUndertakingJourney.next
              } yield redirect
            }
          )
      case None => handleMissingSessionData("Undertaking Journey")

    }
  }

  def getSector: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
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

  def postSector: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[UndertakingJourney].flatMap { previous =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors => {
            val result: OptionT[Future, Result] = for {
              undertakingJourney <- store.get[UndertakingJourney].toContext
              undertakingName <- undertakingJourney.name.value.toContext
            } yield (BadRequest(undertakingSectorPage(errors, previous, undertakingName)))
            result.fold(handleMissingSessionData("Undertaking Journey"))(identity)
          },
          form =>
            for {
              updatedUndertakingJourney <- store.update[UndertakingJourney](updateUndertakingSector(form))
              redirect <- updatedUndertakingJourney.next
            } yield redirect
        )
    }
  }

  def getCheckAnswers: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        Ok(
          cyaPage(
            journey.name.value.fold(throw new IllegalStateException("name should be defined"))(UndertakingName(_)),
            eori,
            journey.sector.value.getOrElse(throw new IllegalStateException("sector should be defined")),
            journey.previous
          )
        ).toFuture
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postCheckAnswers: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form => {
          val result = for {
            updatedJourney <- store.update[UndertakingJourney](updateUndertakingCYA(form)).toContext
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
            undertakingCreated <- createUndertakingAndSendEmail(undertaking, eori, updatedJourney).toContext
          } yield undertakingCreated
          result.fold(handleMissingSessionData("Undertaking create journey"))(identity)
        }
      )
  }

  private def createUndertakingAndSendEmail(
    undertaking: Undertaking,
    eori: EORI,
    undertakingJourney: UndertakingJourney
  )(implicit request: AuthenticatedEscRequest[_]): Future[Result] =
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

  def getConfirmation(ref: String, name: String): Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    Ok(confirmationPage(UndertakingRef(ref), UndertakingName(name))).toFuture
  }

  def postConfirmation: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store
            .update[UndertakingJourney](updateUndertakingConfirmation(form))
            .map { _ =>
              Redirect(routes.BusinessEntityController.getAddBusinessEntity())
            }
      )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
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

  private def updateIsAmendState(value: Boolean)(implicit e: EORI): Future[UndertakingJourney] =
    store.update[UndertakingJourney](jo => jo.map(_.copy(isAmend = value)))

  def postAmendUndertaking: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
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
          result.fold(handleMissingSessionData("Undertaking Journey"))(identity)

        }
      )
  }

  private def ensureUndertakingJourneyPresent(
    journey: Option[UndertakingJourney]
  )(f: UndertakingJourney => Future[Result]): Future[Result] =
    journey match {
      case Some(undertakingJourney) => f(undertakingJourney)
      case None => handleMissingSessionData("Undertaking journey")
    }

  private def updateUndertakingJourney(ujOpt: Option[UndertakingJourney])(f: UndertakingJourney => UndertakingJourney) =
    ujOpt.map(f)

  private def updateUndertakingName(formValues: FormValues)(ujOpt: Option[UndertakingJourney]) =
    updateUndertakingJourney(ujOpt) { journey =>
      journey.copy(name = journey.name.copy(value = Some(formValues.value)))
    }

  private def updateUndertakingSector(formValues: FormValues)(ujOpt: Option[UndertakingJourney]) =
    updateUndertakingJourney(ujOpt) { journey =>
      journey.copy(sector = journey.sector.copy(value = Some(Sector(formValues.value.toInt))))
    }

  private def updateUndertakingCYA(formValues: FormValues)(ujOpt: Option[UndertakingJourney]) =
    updateUndertakingJourney(ujOpt) { journey =>
      journey.copy(cya = journey.cya.copy(value = Some(formValues.value.toBoolean)))
    }

  private def updateUndertakingConfirmation(formValues: FormValues)(ujOpt: Option[UndertakingJourney]) =
    updateUndertakingJourney(ujOpt) { journey =>
      journey.copy(confirmation = journey.confirmation.copy(value = Some(formValues.value.toBoolean)))

    }

  private val undertakingNameForm: Form[FormValues] = Form(
    mapping("undertakingName" -> mandatory("undertakingName"))(FormValues.apply)(FormValues.unapply).verifying(
      "regex.error",
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

}
