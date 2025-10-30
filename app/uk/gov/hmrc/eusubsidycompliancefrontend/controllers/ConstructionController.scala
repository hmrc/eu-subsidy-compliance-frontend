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

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(constructionLvl2Page(constructionLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitConstructionLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    constructionLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(constructionLvl2Page(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
              case None => ""
            }

            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  //CivilEngineeringLvl3Page
  def loadCivilEngineeringLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(civilEngineeringLvl3Page(civilEngineeringLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCivilEngineeringLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    civilEngineeringLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(civilEngineeringLvl3Page(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
              case None => ""
            }

            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  //SpecialisedConstructionLvl3Page
  def loadSpecialisedConstructionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        specialisedConstructionLvl3Page(specialisedConstructionLvl3Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitSpecialisedConstructionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    specialisedConstructionLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(specialisedConstructionLvl3Page(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
              case None => ""
            }

            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  //BuildingCompletionLvl4Page
  def loadBuildingCompletionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(buildingCompletionLvl4Page(buildingCompletionLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        constructionRoadsRailwaysLvl4Page(constructionRoadsRailwaysLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        constructionUtilityProjectsLvl4Page(constructionUtilityProjectsLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        demolitionSitePreparationLvl4Page(demolitionSitePreparationLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        electricalPlumbingConstructionLvl4Page(
          electricalPlumbingConstructionLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        otherCivilEngineeringProjectsLvl4Page(
          otherCivilEngineeringProjectsLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        otherSpecialisedConstructionLvl4Page(
          otherSpecialisedConstructionLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        specialisedConstructionActivitiesLvl4Page(
          specialisedConstructionActivitiesLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
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
