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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, FormPage, NewLeadJourney, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectNewLeadController @Inject()(
     mcc: MessagesControllerComponents,
     escActionBuilders: EscActionBuilders,
     escService: EscService,
     store: Store,
     selectNewLeadPage: SelectNewLeadPage,
     leadEORIChangedPage: LeadEORIChangedPage
)( implicit val appConfig: AppConfig, executionContext: ExecutionContext) extends BaseController(mcc) {

  import escActionBuilders._

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
              store.update[NewLeadJourney] {
                _.map { newLeadJourney =>
                  val updatedLead = newLeadJourney.selectNewLead.copy(value = EORI(form.value).some)
                  newLeadJourney.copy(selectNewLead = updatedLead)
                }
              }.map(_ => Redirect(routes.SelectNewLeadController.getLeadEORIChanged()))

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