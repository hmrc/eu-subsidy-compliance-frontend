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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.lvl1.{GeneralTradeUndertakingOtherPage, GeneralTradeUndertakingPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.lvl2.{ClothesTextilesHomewarePage, ComputersElectronicsMachineryPage, FoodBeveragesTobaccoPage, Lvl2_1GroupsPage, MetalsChemicalsMaterialsPage, PaperPrintedProductsPage, VehiclesTransportPage}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GeneralTradeGroupsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  generalTradeUndertakingPage: GeneralTradeUndertakingPage,
  generalTradeUndertakingOtherPage: GeneralTradeUndertakingOtherPage,
  lvl2_1GroupsPage: Lvl2_1GroupsPage,
  clothesTextilesHomewarePage: ClothesTextilesHomewarePage,
  computersElectronicsMachineryPage: ComputersElectronicsMachineryPage,
  foodBeveragesTobaccoPage: FoodBeveragesTobaccoPage,
  metalsChemicalsMaterialsPage: MetalsChemicalsMaterialsPage,
  paperPrintedProductsPage: PaperPrintedProductsPage,
  vehiclesTransportPage: VehiclesTransportPage,
  naceCheckDetailsController: NACECheckDetailsController
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val generalTradeUndertakingForm: Form[FormValues] = formWithSingleMandatoryField("gt")
  private val generalTradeUndertakingOtherForm: Form[FormValues] = formWithSingleMandatoryField("gt-other")
  private val lvl2_1GroupsForm: Form[FormValues] = formWithSingleMandatoryField("manu-g1")
  private val clothesTextilesHomewareForm: Form[FormValues] = formWithSingleMandatoryField("manu-g2")
  private val computersElectronicsMachineryForm: Form[FormValues] = formWithSingleMandatoryField("manu-g3")
  private val foodBeveragesTobaccoForm: Form[FormValues] = formWithSingleMandatoryField("manu-g4")
  private val metalsChemicalsMaterialsForm: Form[FormValues] = formWithSingleMandatoryField("manu-g5")
  private val paperPrintedProductsForm: Form[FormValues] = formWithSingleMandatoryField("manu-g6")
  private val vehiclesTransportForm: Form[FormValues] = formWithSingleMandatoryField("manu-g7")

  // GeneralTradeUndertakingPage
  def loadGeneralTradeUndertakingPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      val previousLevel1Code =
        if (sector.equals("00") || sector.length < 2) sector else naceCheckDetailsController.deriveLevel1Code(sector)

      Ok(generalTradeUndertakingPage(generalTradeUndertakingForm.fill(FormValues(previousLevel1Code)), journey.mode)).toFuture
    }
  }

  def submitGeneralTradeUndertakingPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingPage(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length >= 2) value.toString.take(2) else value.toString
              case None => ""
            }
            val previousLevel1Code = naceCheckDetailsController.deriveLevel1Code(previousAnswer)
            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousLevel1Code.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else if (previousLevel1Code.equals(form.value))
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

  // GeneralTradeUndertakingOtherPage
  def loadGeneralTradeUndertakingOtherPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }

      val previousLevel1Code =
        if (sector.equals("00") || sector.length < 2) sector
        else naceCheckDetailsController.deriveLevel1Code(sector.take(2))

      Ok(generalTradeUndertakingOtherPage(generalTradeUndertakingOtherForm.fill(FormValues(previousLevel1Code)), journey.mode)).toFuture
    }
  }

  def submitGeneralTradeUndertakingOtherPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingOtherForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingOtherPage(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length >= 2) value.toString.take(2) else value.toString
              case None => ""
            }
            val previousLevel1Code = naceCheckDetailsController.deriveLevel1Code(previousAnswer)
            val lvl4Answer = journey.sector.value match {
              case Some(lvl4Value) => lvl4Value.toString
              case None => ""
            }

            if (previousLevel1Code.equals(form.value) && journey.isNaceCYA)
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else if (previousLevel1Code.equals(form.value))
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

  // Lvl2_1GroupsPage
  def loadLvl2_1GroupsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      Ok(lvl2_1GroupsPage(lvl2_1GroupsForm.fill(FormValues(journey.internalNaceCode)), journey.mode)).toFuture
    }
  }

  def submitLvl2_1GroupsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    lvl2_1GroupsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(lvl2_1GroupsPage(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            if (form.value.equals(journey.internalNaceCode) && journey.isNaceCYA) {
              val lvl4Answer = journey.sector.value match {
                case Some(lvl4Value) => lvl4Value.toString
                case None => ""
              }
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            } else {
              for {
                updatedStoreFlags <- store
                  .update[UndertakingJourney](_.copy(internalNaceCode = form.value, isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  // ClothesTextilesHomewarePage
  def loadClothesTextilesHomewarePage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(clothesTextilesHomewarePage(clothesTextilesHomewareForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitClothesTextilesHomewarePage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clothesTextilesHomewareForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothesTextilesHomewarePage(formWithErrors, "")).toFuture,
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

  // ComputersElectronicsMachineryPage
  def loadComputersElectronicsMachineryPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(computersElectronicsMachineryPage(computersElectronicsMachineryForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitComputersElectronicsMachineryPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    computersElectronicsMachineryForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(computersElectronicsMachineryPage(formWithErrors, "")).toFuture,
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

  // FoodBeveragesTobaccoPage
  def loadFoodBeveragesTobaccoPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(foodBeveragesTobaccoPage(foodBeveragesTobaccoForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFoodBeveragesTobaccoPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodBeveragesTobaccoForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodBeveragesTobaccoPage(formWithErrors, "")).toFuture,
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

  // MetalsChemicalsMaterialsPage
  def loadMetalsChemicalsMaterialsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(metalsChemicalsMaterialsPage(metalsChemicalsMaterialsForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMetalsChemicalsMaterialsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    metalsChemicalsMaterialsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(metalsChemicalsMaterialsPage(formWithErrors, "")).toFuture,
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

  // PaperPrintedProductsPage
  def loadPaperPrintedProductsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(paperPrintedProductsPage(paperPrintedProductsForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPaperPrintedProductsPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    paperPrintedProductsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(paperPrintedProductsPage(formWithErrors, "")).toFuture,
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

  // VehiclesTransportPage
  def loadVehiclesTransportPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }

      Ok(vehiclesTransportPage(vehiclesTransportForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitVehiclesTransportPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    vehiclesTransportForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(vehiclesTransportPage(formWithErrors, "")).toFuture,
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
}
