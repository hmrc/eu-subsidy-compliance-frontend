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

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, OneOf, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, FormPage, Journey, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  undertakingContactPage: UndertakingContactPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  confirmationPage: ConfirmationPage,
  amendUndertakingPage: AmendUndertakingPage
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def firstEmptyPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].map {
      case Some(journey) =>
        journey
          .firstEmpty
          .fold(
            Redirect(routes.BusinessEntityController.getAddBusinessEntity())
          )(identity)
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def getUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
      store.get[UndertakingJourney].flatMap {
        case Some(journey) =>
          val form = journey.name.value.fold(undertakingNameForm)(name => undertakingNameForm.fill(FormValues(name)))
          Future.successful(Ok(undertakingNamePage(form)))

        case None => // initialise the empty Journey model
          store.put(UndertakingJourney()).map { _ =>
            Ok(undertakingNamePage(undertakingNameForm))
          }

      }
  }

  def postUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    undertakingNameForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(undertakingNamePage(errors))),
      success = form => {
        for {
          updatedUndertaking <- store.update[UndertakingJourney]{ _.map { undertakingJourney =>
            val updatedName = undertakingJourney.name.copy(value = Some(form.value))
              undertakingJourney.copy(name = updatedName)
            }
          }
          redirect <- getJourneyNext(updatedUndertaking)
        } yield redirect
      }
    )
  }

  def getSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val form = journey.sector.value.fold(undertakingSectorForm)(sector => undertakingSectorForm.fill(FormValues(sector.id.toString)))
        Future.successful(
          Ok(undertakingSectorPage(
            form,
            getJourneyPrevious(journey),
            journey.name.value.getOrElse("")
          ))
        )
      case _ => handleMissingSessionData("Undertaking journey")

    }
  }

  def postSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      undertakingSectorForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(undertakingSectorPage(errors, previous, ""))),
        form => {
          for {
            updatedUndertaking <-  store.update[UndertakingJourney]{ _.map { undertakingJourney =>
              val updatedSector = undertakingJourney.sector.copy(value = Some(Sector(form.value.toInt)))
                undertakingJourney.copy(sector = updatedSector)
              }
            }
            redirect <- getJourneyNext(updatedUndertaking)
          } yield redirect
        }
      )
    }
  }

  def getContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val form = journey.contact.value.fold(contactForm)(contactDetails => contactForm.fill(OneOf(contactDetails.phone.map(_.toString), contactDetails.mobile.map(_.toString))))
        Future.successful(
          Ok(undertakingContactPage(
            form,
            getJourneyPrevious(journey),
            journey.name.value.getOrElse("")
          ))
        )
      case _ => handleMissingSessionData("Undertaking journey")

    }
  }

  def postContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      contactForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(undertakingContactPage(errors, previous, ""))),
        form => {
          for {
            updatedUndertaking <- store.update[UndertakingJourney] {
              _.map { undertakingJourney =>
                val updatedContact = undertakingJourney.contact.copy(value = Some(form.toContactDetails))
                undertakingJourney.copy(contact = updatedContact)
              }
            }
            redirect <- getJourneyNext(updatedUndertaking)
          } yield redirect

        }
      )
    }
  }

  def getCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        Future.successful(
          Ok(
            cyaPage(
              journey.name.value.fold(throw new IllegalStateException("name should be defined"))(UndertakingName(_)),
              eori,
              journey.sector.value.getOrElse(throw new IllegalStateException("sector should be defined")),
              journey.contact.value.getOrElse(throw new IllegalStateException("contact should be defined")),
              journey.previous
            )
          )
        )
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[UndertakingJourney]({ x =>
          x.map { y =>
            y.copy(cya = y.cya.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap { journey: UndertakingJourney =>
          for {
            ref <- escService.createUndertaking(
                    Undertaking(
                      None,
                      name = UndertakingName(journey.name.value.getOrElse(throw new IllegalThreadStateException(""))),
                      industrySector = journey.sector.value.getOrElse(throw new IllegalThreadStateException("")),
                      None,
                      None,
                      List(BusinessEntity(eori, leadEORI = true, journey.contact.value)
                    )))

          } yield {
            Redirect(routes.UndertakingController.getConfirmation(ref, journey.name.value.getOrElse("")))
          }
        }
      }
    )
  }

  def getConfirmation(
    ref: String,
    name: String
  ): Action[AnyContent] = escAuthentication.async { implicit request =>
      Future.successful(Ok(confirmationPage(UndertakingRef(ref), UndertakingName(name))))
  }

  def postConfirmation: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[UndertakingJourney]({ x =>
          x.map { y =>
            y.copy(confirmation = y.confirmation.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap{ _ =>
          Future.successful(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))
        }
      }
    )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        for {
          updatedJourney <- if (journey.isAmend.value.contains(true)) Future.successful(journey) else {
            store.update[UndertakingJourney] {
              _.map { undertakingJourney =>
                val updatedAmend = undertakingJourney.isAmend.copy(value = true.some)
                undertakingJourney.copy(isAmend = updatedAmend)
              }
            }
          }
        } yield (
          Ok(
            amendUndertakingPage(
              updatedJourney.name.value.fold(sys.error("name should be defined"))(UndertakingName(_)),
              updatedJourney.sector.value.getOrElse(sys.error("sector should be defined")),
              updatedJourney.contact.value.getOrElse(sys.error("contact should be defined")),
              routes.AccountController.getAccountPage().url
            )
          )
          )
      case None => sys.error(" undertaking not retrieved")
    }
  }

    def postAmendUndertaking: Action[AnyContent] =  escAuthentication.async { implicit  request =>
      implicit val eori: EORI = request.eoriNumber
      Future.successful(Ok(s""))
  }

  private def isAmend(journey: UndertakingJourney) = journey.isAmend.value.contains(true)
  private def getJourneyPrevious(journey: UndertakingJourney)(implicit request: Request[_]) =
    if(isAmend(journey)) routes.UndertakingController.getAmendUndertakingDetails().url else journey.previous

  private def getJourneyNext(journey: UndertakingJourney)(implicit request: Request[_]) =
    if(isAmend(journey)) Future.successful(Redirect(routes.UndertakingController.getAmendUndertakingDetails())) else journey.next

  lazy val eoriCheckForm : Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply))

  lazy val createUndertakingForm: Form[FormValues] = Form(
    mapping("createUndertaking" -> mandatory("createUndertaking"))(FormValues.apply)(FormValues.unapply))

  lazy val undertakingNameForm: Form[FormValues] = Form(
    mapping("undertakingName" -> mandatory("undertakingName"))(FormValues.apply)(FormValues.unapply).verifying(
      "regex.error",
      fields => fields match {
        case a if a.value.matches(UndertakingName.regex) => true
        case _ => false
      }
    ))

  lazy val undertakingSectorForm: Form[FormValues] = Form(
    mapping("undertakingSector" -> mandatory("undertakingSector"))(FormValues.apply)(FormValues.unapply))

  lazy val cyaForm: Form[FormValues] = Form(
    mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

  lazy val confirmationForm: Form[FormValues] = Form(
    mapping("confirm" -> mandatory("confirm"))(FormValues.apply)(FormValues.unapply))

  lazy val amendUndertakingForm: Form[FormValues] = Form(
    mapping("amendUndertaking" -> mandatory("amendUndertaking"))(FormValues.apply)(FormValues.unapply))

}
