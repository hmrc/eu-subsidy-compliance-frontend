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

import cats.implicits.{catsSyntaxOptionId}
import play.api.Configuration
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, OneOf, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, JourneyTraverseService, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.emailSend.EmailParameters.SingleEORIEmailParameter
import uk.gov.hmrc.eusubsidycompliancefrontend.models.emailSend.EmailSendResult
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Language, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, RetrieveEmailService, SendEmailService, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  sendEmailService: SendEmailService,
  configuration: Configuration,
  retrieveEmailService: RetrieveEmailService,
  journeyTraverseService: JourneyTraverseService,
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

  val  templateIdEN = configuration.get[String]("email-send.create-undertaking-template-en")
  val  templateIdCY = configuration.get[String]("email-send.create-undertaking-template-cy")

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
      ensureUndertakingJourneyPresent(_) { journey =>
        val form = journey.sector.value.fold(undertakingSectorForm)(sector => undertakingSectorForm.fill(FormValues(sector.id.toString)))
        Future.successful(
          Ok(undertakingSectorPage(
            form,
            getJourneyPrevious(journey),
            journey.name.value.getOrElse("")
          ))
        )
      }
    }
  }

  def postSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[UndertakingJourney].flatMap { previous =>
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
          ensureUndertakingJourneyPresent(_) { journey =>
            val form = journey.contact.value.fold(contactForm)(contactDetails =>
              contactForm.fill(OneOf(contactDetails.phone.map(_.toString), contactDetails.mobile.map(_.toString))))
            Future.successful(
              Ok(undertakingContactPage(
                form,
                getJourneyPrevious(journey),
                journey.name.value.getOrElse("")
              ))
            )
          }
    }
  }

  def postContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[UndertakingJourney].flatMap { previous =>
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
              List(BusinessEntity(eori, leadEORI = true, updatedJourney.contact.value)
              ))
            ref <- escService.createUndertaking(undertaking)
           result <- createUndertakingAndSendEmail(undertaking, eori, updatedJourney)

          } yield result
    )
  }

  //This method creates undertaking, checks for the language, fetches the appropriate template as per the lang
  //call the retrieve email service and sends the email to retrieved email address
  private def createUndertakingAndSendEmail(undertaking: Undertaking, eori: EORI, undertakingJourney: UndertakingJourney)(implicit request: EscAuthRequest[_]) =     for {
    ref <- escService.createUndertaking(undertaking)
    lang <- getLanguage
    templateId = getTemplateId(lang)
    emailParameters = SingleEORIEmailParameter(eori, undertaking.name, ref,  "undertaking Created by Lead EORI")
    emailAddress <- retrieveEmailService.retrieveEmailByEORI(eori).map(_.getOrElse(sys.error("Email won't be send as email address is not present")))
  } yield {
    val _ = sendEmailService.sendEmail(emailAddress, emailParameters, templateId)
    Redirect(routes.UndertakingController.getConfirmation(ref, undertakingJourney.name.value.getOrElse("")))
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
        store.update[UndertakingJourney]{ _.map { undertakingJourney =>
            undertakingJourney.copy(confirmation = undertakingJourney.confirmation.copy(value = Some(form.value.toBoolean)))
          }
        }.flatMap { _ =>
          Future.successful(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))
        }
      }
    )
  }

  def getAmendUndertakingDetails: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[UndertakingJourney].flatMap {
      ensureUndertakingJourneyPresent(_) { journey =>
        for {
          updatedJourney <- if (isAmend(journey)) Future.successful(journey) else
            store.update[UndertakingJourney] (_.map ( _.copy(isAmend = true.some)))
        } yield Ok(
          amendUndertakingPage(
            updatedJourney.name.value.fold(handleMissingSessionData("Undertaking Name"))(UndertakingName(_)),
            updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
            updatedJourney.contact.value.getOrElse(handleMissingSessionData("Undertaking contact")),
            routes.AccountController.getAccountPage().url
          )
        )

      }
    }
  }

    def postAmendUndertaking: Action[AnyContent] =  escAuthentication.async { implicit  request =>
      implicit val eori: EORI = request.eoriNumber

      amendUndertakingForm.bindFromRequest().fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        _ =>
            for {
              updatedJourney <- store.update[UndertakingJourney] (_.map ( _.copy(isAmend = None)))
              undertakingName = UndertakingName(updatedJourney.name.value.getOrElse(handleMissingSessionData("Undertaking Name")))
              undertakingSector = updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking Sector"))
              retrievedUndertaking <- escService.retrieveUndertaking(eori).map(_.getOrElse(handleMissingSessionData("Undertaking")))
              undertakingRef = retrievedUndertaking.reference.getOrElse(handleMissingSessionData("Undertaking ref"))
              businessEntityList = retrievedUndertaking.undertakingBusinessEntity
              leadBEList = businessEntityList.filter(_.leadEORI)
              leadBE = if (leadBEList.nonEmpty) leadBEList.head else handleMissingSessionData("lead Business Entity")
              updatedLeadBE = leadBE.copy(contacts = updatedJourney.contact.value)
              updatedUndertaking = retrievedUndertaking.copy(name = undertakingName, industrySector = undertakingSector)
              _ <- escService.updateUndertaking(updatedUndertaking)
              _ <- escService.addMember(undertakingRef, updatedLeadBE)
            } yield Redirect(routes.AccountController.getAccountPage())
        )
  }

  private def isAmend(journey: UndertakingJourney) = journey.isAmend.contains(true)

  private def getJourneyPrevious(journey: UndertakingJourney)(implicit request: Request[_]) =
    if(isAmend(journey)) routes.UndertakingController.getAmendUndertakingDetails().url else journey.previous

  private def getJourneyNext(journey: UndertakingJourney)(implicit request: Request[_]) =
    if(isAmend(journey)) Future.successful(Redirect(routes.UndertakingController.getAmendUndertakingDetails())) else journey.next

  private def ensureUndertakingJourneyPresent(journey: Option[UndertakingJourney])(f: UndertakingJourney => Future[Result]): Future[Result] = {
    journey match {
      case Some(undertakingJourney) => f(undertakingJourney)
      case None => handleMissingSessionData("Undertaking journey")
    }
  }

  private def getLanguage(implicit request: EscAuthRequest[_]): Future[Language] = request.request.messages.lang.code.toLowerCase(Locale.UK) match {
    case English.code => Future.successful(English)
    case Welsh.code   => Future.successful(Welsh)
    case other        => sys.error(s"Found unsupported language code $other")
  }

  private def getTemplateId(lang: Language) = lang match {
    case English => templateIdEN
    case Welsh   => templateIdCY
  }

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
