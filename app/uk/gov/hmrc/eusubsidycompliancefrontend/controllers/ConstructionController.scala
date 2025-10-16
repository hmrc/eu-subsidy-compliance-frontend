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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
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
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val constructionLvl2Form: Form[FormValues] = formWithSingleMandatoryField("construction2")
  private val civilEngineeringLvl3Form: Form[FormValues] = formWithSingleMandatoryField("civilEngineering3")
  private val specialisedConstructionLvl3Form: Form[FormValues] = formWithSingleMandatoryField("special3")
  private val buildingCompletionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("building4")
  private val constructionRoadsRailwaysLvl4Form: Form[FormValues] = formWithSingleMandatoryField("roads4")
  private val constructionUtilityProjectsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("utility4")
  private val demolitionSitePreparationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("demo4")
  private val electricalPlumbingConstructionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("plumbing4")
  private val otherCivilEngineeringProjectsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherCivil4")
  private val otherSpecialisedConstructionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherSpecial4")
  private val specialisedConstructionActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("special4")

  //ConstructionLvl2Page
  def loadConstructionLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(constructionLvl2Page(constructionLvl2Form, "")).toFuture
  }

  def submitConstructionLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    constructionLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(constructionLvl2Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //CivilEngineeringLvl3Page
  def loadCivilEngineeringLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(civilEngineeringLvl3Page(civilEngineeringLvl3Form, "")).toFuture
  }

  def submitCivilEngineeringLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    civilEngineeringLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(civilEngineeringLvl3Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //SpecialisedConstructionLvl3Page
  def loadSpecialisedConstructionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(specialisedConstructionLvl3Page(specialisedConstructionLvl3Form, "")).toFuture
  }

  def submitSpecialisedConstructionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    specialisedConstructionLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(specialisedConstructionLvl3Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //BuildingCompletionLvl4Page
  def loadBuildingCompletionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(buildingCompletionLvl4Page(buildingCompletionLvl4Form, "")).toFuture
  }

  def submitBuildingCompletionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    buildingCompletionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(buildingCompletionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //ConstructionRoadsRailwaysLvl4Page
  def loadConstructionRoadsRailwaysLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(constructionRoadsRailwaysLvl4Page(constructionRoadsRailwaysLvl4Form, "")).toFuture
  }

  def submitConstructionRoadsRailwaysLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    constructionRoadsRailwaysLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(constructionRoadsRailwaysLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //ConstructionUtilityProjectsLvl4Page
  def loadConstructionUtilityProjectsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(constructionUtilityProjectsLvl4Page(constructionUtilityProjectsLvl4Form, "")).toFuture
  }

  def submitConstructionUtilityProjectsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    constructionUtilityProjectsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(constructionUtilityProjectsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //DemolitionSitePreparationLvl4Page
  def loadDemolitionSitePreparationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(demolitionSitePreparationLvl4Page(demolitionSitePreparationLvl4Form, "")).toFuture
  }

  def submitDemolitionSitePreparationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    demolitionSitePreparationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(demolitionSitePreparationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //ElectricalPlumbingConstructionLvl4Page
  def loadElectricalPlumbingConstructionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(electricalPlumbingConstructionLvl4Page(electricalPlumbingConstructionLvl4Form, "")).toFuture
  }

  def submitElectricalPlumbingConstructionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    electricalPlumbingConstructionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(electricalPlumbingConstructionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //OtherCivilEngineeringProjectsLvl4Page
  def loadOtherCivilEngineeringProjectsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherCivilEngineeringProjectsLvl4Page(otherCivilEngineeringProjectsLvl4Form, "")).toFuture
  }

  def submitOtherCivilEngineeringProjectsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherCivilEngineeringProjectsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherCivilEngineeringProjectsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //OtherSpecialisedConstructionLvl4Page
  def loadOtherSpecialisedConstructionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherSpecialisedConstructionLvl4Page(otherSpecialisedConstructionLvl4Form, "")).toFuture
  }

  def submitOtherSpecialisedConstructionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherSpecialisedConstructionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherSpecialisedConstructionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //SpecialisedConstructionActivitiesLvl4Page
  def loadSpecialisedConstructionActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(specialisedConstructionActivitiesLvl4Page(specialisedConstructionActivitiesLvl4Form, "")).toFuture
  }

  def submitSpecialisedConstructionActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    specialisedConstructionActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(specialisedConstructionActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
}
