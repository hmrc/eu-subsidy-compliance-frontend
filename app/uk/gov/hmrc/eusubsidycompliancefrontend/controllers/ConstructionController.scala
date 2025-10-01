/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.construction.{BuildingCompletionLvl4Page, CivilEngineeringLvl3Page, ConstructionLvl2Page, ConstructionRoadsRailwaysLvl4Page, ConstructionUtilityProjectsLvl4Page, DemolitionSitePreparationLvl4Page, ElectricalPlumbingConstructionLvl4Page, OtherCivilEngineeringProjectsLvl4Page, OtherSpecialisedConstructionLvl4Page, SpecialisedConstructionActivitiesLvl4Page, SpecialisedConstructionLvl3Page}

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class ConstructionController @Inject() (
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             buildingCompletionLvl4Page: BuildingCompletionLvl4Page,
                                             civilEngineeringLvl3Page: CivilEngineeringLvl3Page,
                                             constructionLvl2Page: ConstructionLvl2Page,
                                             constructionRoadsRailwaysLvl4Page: ConstructionRoadsRailwaysLvl4Page,
                                             constructionUtilityProjectsLvl4Page: ConstructionUtilityProjectsLvl4Page,
                                             demolitionSitePreparationLvl4Page: DemolitionSitePreparationLvl4Page,
                                             electricalPlumbingConstructionLvl4Page: ElectricalPlumbingConstructionLvl4Page,
                                             otherCivilEngineeringProjectsLvl4Page: OtherCivilEngineeringProjectsLvl4Page,
                                             otherSpecialisedConstructionLvl4Page: OtherSpecialisedConstructionLvl4Page,
                                             specialisedConstructionActivitiesLvl4Page: SpecialisedConstructionActivitiesLvl4Page,
                                             specialisedConstructionLvl3Page: SpecialisedConstructionLvl3Page
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val constructionLvl2Form: Form[FormValues] = formWithSingleMandatoryField("constructionLvl2")
  private val civilEngineeringLvl3Form: Form[FormValues] = formWithSingleMandatoryField("civilEngineeringLvl3")
  private val specialisedConstructionLvl3Form: Form[FormValues] = formWithSingleMandatoryField("specialisedConstructionLvl3")

  def loadConstructionLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(constructionLvl2Page(constructionLvl2Form)).toFuture
  }

  //submitPage()
  def submitConstructionLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    constructionLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(constructionLvl2Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadCivilEngineeringLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(civilEngineeringLvl3Page(civilEngineeringLvl3Form)).toFuture
  }

  //submitPage()
  def submitCivilEngineeringLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    civilEngineeringLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(civilEngineeringLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
}