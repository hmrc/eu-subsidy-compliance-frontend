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

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  sendEmailHelperService: SendEmailHelperService,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  confirmationPage: ConfirmationPage,
  amendUndertakingPage: AmendUndertakingPage
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._
   val CreateUndertaking = "createUndertaking"

  def firstEmptyPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].map {
      case Some(journey) =>
        journey
          .firstEmpty
          .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))(identity)
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def getUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
      store.get[UndertakingJourney].flatMap {
        case Some(journey) =>
          val form = journey.name.value.fold(undertakingNameForm)(name => undertakingNameForm.fill(FormValues(name)))
          Ok(undertakingNamePage(form)).toFuture
        case None => // initialise the empty Journey model
          store.put(UndertakingJourney()).map { _ =>
            Ok(undertakingNamePage(undertakingNameForm))
          }
      }
  }

  def postUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    undertakingNameForm.bindFromRequest().fold(
      errors => BadRequest(undertakingNamePage(errors)).toFuture,
      success = form => {
        for {
          updatedUndertakingJourney <- store.update[UndertakingJourney]{ _.map { undertakingJourney =>
            val updatedName = undertakingJourney.name.copy(value = Some(form.value))
              undertakingJourney.copy(name = updatedName)
            }
          }
          redirect <- updatedUndertakingJourney.next
        } yield redirect
      }
    )
  }

  def getSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        val form = journey.sector.value.fold(undertakingSectorForm)(sector => undertakingSectorForm.fill(FormValues(sector.id.toString)))
          Ok(undertakingSectorPage(
            form,
            journey.previous,
            journey.name.value.getOrElse("")
          )).toFuture
      }
    }
  }

  def postSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[UndertakingJourney].flatMap { previous =>
      undertakingSectorForm.bindFromRequest().fold(
        errors => BadRequest(undertakingSectorPage(errors, previous, "")).toFuture,
        form => {
          for {
            updatedUndertakingJourney <-  store.update[UndertakingJourney]{ _.map { undertakingJourney =>
              val updatedSector = undertakingJourney.sector.copy(value = Some(Sector(form.value.toInt)))
                undertakingJourney.copy(sector = updatedSector)
              }
            }
            redirect <- updatedUndertakingJourney.next
          } yield redirect
        }
      )
    }
  }


  def getCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) => Ok(cyaPage(
        journey.name.value.fold(throw new IllegalStateException("name should be defined"))(UndertakingName(_)),
        eori,
        journey.sector.value.getOrElse(throw new IllegalStateException("sector should be defined")),
        journey.previous
      )).toFuture
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form =>
          for {
            updatedJourney <-  store.update[UndertakingJourney]{ _.map { undertakingJourney =>
                undertakingJourney.copy(cya = undertakingJourney.cya.copy(value = Some(form.value.toBoolean)))
              }
            }
            undertakingName = UndertakingName(updatedJourney.name.value.getOrElse(handleMissingSessionData("Undertaking Name")))
            undertakingSector = updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking Sector"))
            undertaking =  Undertaking(
              None,
              name = undertakingName,
              industrySector = undertakingSector,
              None,
              None,
              List(BusinessEntity(eori, leadEORI = true)
              ))
            result <- createUndertakingAndSendEmail(undertaking, eori, updatedJourney)

          } yield result

    )
  }

  private def createUndertakingAndSendEmail(
    undertaking: Undertaking,
    eori: EORI,
    undertakingJourney: UndertakingJourney
  )(implicit request: EscAuthRequest[_]): Future[Result] = {
    for {
      ref <- escService.createUndertaking(undertaking)
     _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(eori, None, CreateUndertaking, undertaking, ref, None)
    } yield {
      Redirect(routes.UndertakingController.getConfirmation(ref, undertakingJourney.name.value.getOrElse("")))
    }
  }


  def getConfirmation(ref: String, name: String): Action[AnyContent] = escAuthentication.async { implicit request =>
    Ok(confirmationPage(UndertakingRef(ref), UndertakingName(name))).toFuture
  }

  def postConfirmation: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[UndertakingJourney]{ _.map { undertakingJourney =>
            undertakingJourney.copy(confirmation = undertakingJourney.confirmation.copy(value = Some(form.value.toBoolean)))
          }
        }.flatMap { _ =>
          Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
        }
      }
    )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = escAuthentication.async { implicit request =>
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

  def postAmendUndertaking: Action[AnyContent] =  escAuthentication.async { implicit  request =>
    implicit val eori: EORI = request.eoriNumber

    amendUndertakingForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      _ =>
          for {
            updatedJourney <- updateIsAmendState(value = false)
            undertakingName = UndertakingName(updatedJourney.name.value.getOrElse(handleMissingSessionData("Undertaking Name")))
            undertakingSector = updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking Sector"))
            retrievedUndertaking <- escService.retrieveUndertaking(eori).map(_.getOrElse(handleMissingSessionData("Undertaking")))
            undertakingRef = retrievedUndertaking.reference.getOrElse(handleMissingSessionData("Undertaking ref"))
            businessEntityList = retrievedUndertaking.undertakingBusinessEntity
            leadBEList = businessEntityList.filter(_.leadEORI)
            leadBE = if (leadBEList.nonEmpty) leadBEList.head else handleMissingSessionData("lead Business Entity")
            updatedUndertaking = retrievedUndertaking.copy(name = undertakingName, industrySector = undertakingSector)
            _ <- escService.updateUndertaking(updatedUndertaking)
            _ <- escService.addMember(undertakingRef, leadBE)
          } yield Redirect(routes.AccountController.getAccountPage())
      )
  }

  private def ensureUndertakingJourneyPresent(journey: Option[UndertakingJourney])(f: UndertakingJourney => Future[Result]): Future[Result] = {
    journey match {
      case Some(undertakingJourney) => f(undertakingJourney)
      case None => handleMissingSessionData("Undertaking journey")
    }
  }

  private val undertakingNameForm: Form[FormValues] = Form(
    mapping("undertakingName" -> mandatory("undertakingName"))(FormValues.apply)(FormValues.unapply).verifying(
      "regex.error",
      fields => fields match {
        case a if a.value.matches(UndertakingName.regex) => true
        case _ => false
      }
    ))

  private val undertakingSectorForm: Form[FormValues] = Form(
    mapping("undertakingSector" -> mandatory("undertakingSector"))(FormValues.apply)(FormValues.unapply))

  private val cyaForm: Form[FormValues] = Form(
    mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

  private val confirmationForm: Form[FormValues] = Form(
    mapping("confirm" -> mandatory("confirm"))(FormValues.apply)(FormValues.unapply))

  private val amendUndertakingForm: Form[FormValues] = Form(
    mapping("amendUndertaking" -> mandatory("amendUndertaking"))(FormValues.apply)(FormValues.unapply))

}
