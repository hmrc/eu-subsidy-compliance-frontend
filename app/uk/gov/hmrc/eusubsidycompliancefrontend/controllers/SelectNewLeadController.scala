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

import cats.implicits.catsSyntaxOptionId
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIEmailParameter, SingleEORIEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, NewLeadJourney, RetrieveEmailService, SendEmailService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.EmailTemplateHelpers
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectNewLeadController @Inject()(
     mcc: MessagesControllerComponents,
     escActionBuilders: EscActionBuilders,
     escService: EscService,
     store: Store,
     emailTemplateHelpers: EmailTemplateHelpers,
     retrieveEmailService: RetrieveEmailService,
     sendEmailService: SendEmailService,
     configuration: Configuration,
     selectNewLeadPage: SelectNewLeadPage,
     leadEORIChangedPage: LeadEORIChangedPage
)( implicit val appConfig: AppConfig, executionContext: ExecutionContext) extends BaseController(mcc) {

  import escActionBuilders._

  val promoteOtherAsLeadEmailToBusinessEntity = "promoteAsLeadEmailToBE"
  val promoteOtherAsLeadEmailToLead = "promoteAsLeadEmailToLead"

  def getSelectNewLead: Action[AnyContent] = escAuthentication.async { implicit request =>
    val previous = routes.AccountController.getAccountPage().url
    implicit val eori = request.eoriNumber
    (for {
      newLeadJourneyOpt <- store.get[NewLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield (newLeadJourneyOpt, undertakingOpt) match {
      case (Some(newLeadJourney), Some(undertaking)) =>
        val form = newLeadJourney.selectNewLead.value.fold(selectNewLeadForm)(eori => selectNewLeadForm.fill(FormValues(eori.toString)))
        Future.successful(Ok(selectNewLeadPage(form, previous, undertaking.name, undertaking.getAllNonLeadEORIs())))

      case (None, Some(undertaking)) =>
        store.put(NewLeadJourney()).map { journey =>
          val form = journey.selectNewLead.value.fold(selectNewLeadForm)(eori => selectNewLeadForm.fill(FormValues(eori.toString)))
          Ok(selectNewLeadPage(form, previous, undertaking.name, undertaking.getAllNonLeadEORIs()))
        }

      case _ => handleMissingSessionData("Undertaking journey")
    }).flatten
  }


  def postSelectNewLead: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    escService.retrieveUndertaking(eori).flatMap {
      _ match {
        case Some(undertaking) =>
          selectNewLeadForm.bindFromRequest().fold(
            errors => Future.successful(BadRequest(selectNewLeadPage(errors, previous, undertaking.name, undertaking.getAllNonLeadEORIs()))),
            form => {
              val eoriBE = EORI(form.value)
              val undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("Undertaking Ref"))
              for {
                _ <-  store.update[NewLeadJourney] {
                  _.map { newLeadJourney =>
                    val updatedLead = newLeadJourney.selectNewLead.copy(value = eoriBE.some)
                    newLeadJourney.copy(selectNewLead = updatedLead)
                  }
                }
                emailAddressBE <- retrieveEmailService.retrieveEmailByEORI(eoriBE).map(_.getOrElse(handleMissingSessionData(" BE Email Address")))
                emailAddressLead <- retrieveEmailService.retrieveEmailByEORI(eori).map(_.getOrElse(handleMissingSessionData("Lead Email Address")))
                templateIdBE = emailTemplateHelpers.getEmailTemplateId(configuration, promoteOtherAsLeadEmailToBusinessEntity)
                templateIdLead = emailTemplateHelpers.getEmailTemplateId(configuration, promoteOtherAsLeadEmailToLead)
                emailParametersBE = SingleEORIEmailParameter(eoriBE, undertaking.name, undertakingRef,  "Email to BE for being promoted  as a Lead")
                emailParametersLead = DoubleEORIEmailParameter(eori, eoriBE,  undertaking.name, undertakingRef,  "Email to Lead confirming they have assigned other Business Entity as lead")

              } yield {
                sendEmailService.sendEmail(emailAddressBE, emailParametersBE, templateIdBE)
                sendEmailService.sendEmail(emailAddressLead, emailParametersLead, templateIdLead)
                Redirect(routes.SelectNewLeadController.getLeadEORIChanged())
              }

            }
          )
        case _ => handleMissingSessionData("Undertaking journey")
      }
    }
  }

  def getLeadEORIChanged = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[NewLeadJourney].flatMap {
      _ match {
        case Some(newLeadJourney) =>
          for {
            undertaking <- escService.retrieveUndertaking(eori).map(_.getOrElse(handleMissingSessionData("Undertaking")))
            selectedEORI: EORI = newLeadJourney.selectNewLead.value.getOrElse(handleMissingSessionData("selected EORI"))
            _ <- store.update[BusinessEntityJourney]{businessEntityOpt =>
              businessEntityOpt.map(_.copy(isLeadSelectJourney = None))
            }
            _ <- store.put[NewLeadJourney](NewLeadJourney())
          } yield Ok(leadEORIChangedPage(selectedEORI, undertaking.name))

        case None => handleMissingSessionData("New Lead journey")
      }
    }
  }

  lazy val selectNewLeadForm: Form[FormValues] = Form(
    mapping("selectNewLead" -> mandatory("selectNewLead"))(FormValues.apply)(FormValues.unapply))


}
