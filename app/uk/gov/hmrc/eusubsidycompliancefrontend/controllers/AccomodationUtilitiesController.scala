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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.accomodation._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.electricity._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.water._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AccomodationUtilitiesController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AccommodationFoodLvl2Page: AccommodationFoodLvl2Page,
  AccommodationLvl3Page: AccommodationLvl3Page,
  EventCateringOtherFoodActivitiesLvl4Page: EventCateringOtherFoodActivitiesLvl4Page,
  FoodBeverageActivitiesLvl3Page: FoodBeverageActivitiesLvl3Page,
  RestaurantFoodServicesLvl4Page: RestaurantFoodServicesLvl4Page,
  ElectricityLvl3Page: ElectricityLvl3Page,
  ElectricityLvl4Page: ElectricityLvl4Page,
  GasManufactureLvl4Page: GasManufactureLvl4Page,
  WasteCollectionLvl4Page: WasteCollectionLvl4Page,
  WasteCollectionRecoveryLvl3Page: WasteCollectionRecoveryLvl3Page,
  WasteDisposalLvl4Page: WasteDisposalLvl4Page,
  WasteRecoveryLvl4Page: WasteRecoveryLvl4Page,
  WaterLvl2Page: WaterLvl2Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AccommodationFoodLvl2Form: Form[FormValues] = formWithSingleMandatoryField("accommodation2")
  private val AccommodationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("accommodation3")
  private val EventCateringOtherFoodActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("catering4")
  private val FoodBeverageActivitiesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("foodActs3")
  private val RestaurantFoodServicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("restaurant4")
  private val ElectricityLvl3Form: Form[FormValues] = formWithSingleMandatoryField("electricity3")
  private val ElectricityLvl4Form: Form[FormValues] = formWithSingleMandatoryField("electricity4")
  private val GasManufactureLvl4Form: Form[FormValues] = formWithSingleMandatoryField("gas4")
  private val WasteCollectionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteCollection4")
  private val WasteCollectionRecoveryLvl3Form: Form[FormValues] = formWithSingleMandatoryField("wasteCollection3")
  private val WasteDisposalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteDisposal4")
  private val WasteRecoveryLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteRecovery4")
  private val WaterLvl2Form: Form[FormValues] = formWithSingleMandatoryField("water2")

  def loadAccommodationFoodLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      val form = if (sector == "I") AccommodationFoodLvl2Form else AccommodationFoodLvl2Form.fill(FormValues(sector))
      Ok(AccommodationFoodLvl2Page(form, journey.mode)).toFuture
    }
  }

  def submitAccommodationFoodLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AccommodationFoodLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AccommodationFoodLvl2Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadAccommodationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") AccommodationLvl3Form else AccommodationLvl3Form.fill(FormValues(sector))
      Ok(AccommodationLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitAccommodationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AccommodationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AccommodationLvl3Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadEventCateringOtherFoodActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form =
        if (sector == "") EventCateringOtherFoodActivitiesLvl4Form
        else EventCateringOtherFoodActivitiesLvl4Form.fill(FormValues(sector))
      Ok(EventCateringOtherFoodActivitiesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitEventCateringOtherFoodActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EventCateringOtherFoodActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EventCateringOtherFoodActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadFoodBeverageActivitiesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form =
        if (sector == "") FoodBeverageActivitiesLvl3Form else FoodBeverageActivitiesLvl3Form.fill(FormValues(sector))
      Ok(FoodBeverageActivitiesLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitFoodBeverageActivitiesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FoodBeverageActivitiesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FoodBeverageActivitiesLvl3Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadRestaurantFoodServicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form =
        if (sector == "") RestaurantFoodServicesLvl4Form else RestaurantFoodServicesLvl4Form.fill(FormValues(sector))
      Ok(RestaurantFoodServicesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitRestaurantFoodServicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RestaurantFoodServicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RestaurantFoodServicesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadElectricityLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") ElectricityLvl3Form else ElectricityLvl3Form.fill(FormValues(sector))
      Ok(ElectricityLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitElectricityLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricityLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ElectricityLvl3Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadElectricityLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") ElectricityLvl4Form else ElectricityLvl4Form.fill(FormValues(sector))
      Ok(ElectricityLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitElectricityLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricityLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ElectricityLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadGasManufactureLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") GasManufactureLvl4Form else GasManufactureLvl4Form.fill(FormValues(sector))
      Ok(GasManufactureLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitGasManufactureLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    GasManufactureLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(GasManufactureLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWasteCollectionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") WasteCollectionLvl4Form else WasteCollectionLvl4Form.fill(FormValues(sector))
      Ok(WasteCollectionLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitWasteCollectionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteCollectionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteCollectionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWasteCollectionRecoveryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form =
        if (sector == "") WasteCollectionRecoveryLvl3Form else WasteCollectionRecoveryLvl3Form.fill(FormValues(sector))
      Ok(WasteCollectionRecoveryLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitWasteCollectionRecoveryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteCollectionRecoveryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteCollectionRecoveryLvl3Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadWasteDisposalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") WasteDisposalLvl4Form else WasteDisposalLvl4Form.fill(FormValues(sector))
      Ok(WasteDisposalLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitWasteDisposalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteDisposalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteDisposalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWasteRecoveryLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") WasteRecoveryLvl4Form else WasteRecoveryLvl4Form.fill(FormValues(sector))
      Ok(WasteRecoveryLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitWasteRecoveryLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteRecoveryLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteRecoveryLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWaterLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      val form = if (sector == "") WaterLvl2Form else WaterLvl2Form.fill(FormValues(sector))
      Ok(WaterLvl2Page(form, journey.mode)).toFuture
    }
  }

  def submitWaterLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WaterLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WaterLvl2Page(formWithErrors, "")).toFuture,
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
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }
}
