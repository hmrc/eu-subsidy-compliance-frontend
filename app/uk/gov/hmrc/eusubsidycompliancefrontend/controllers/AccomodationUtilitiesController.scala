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

import javax.inject.Inject

class AccomodationUtilitiesController @Inject()(
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
                                             WaterLvl2Page: WaterLvl2Page,

                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val  AccommodationFoodLvl2Form: Form[FormValues] = formWithSingleMandatoryField("accommodation2")
  private val  AccommodationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("accommodation3")
  private val  EventCateringOtherFoodActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("catering4")
  private val  FoodBeverageActivitiesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("foodActs3")
  private val  RestaurantFoodServicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("restaurant4")
  private val  ElectricityLvl3Form: Form[FormValues] = formWithSingleMandatoryField("electricity3")
  private val  ElectricityLvl4Form: Form[FormValues] = formWithSingleMandatoryField("electricity4")
  private val  GasManufactureLvl4Form: Form[FormValues] = formWithSingleMandatoryField("gas4")
  private val  WasteCollectionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteCollection4")
  private val  WasteCollectionRecoveryLvl3Form: Form[FormValues] = formWithSingleMandatoryField("wasteCollection3")
  private val  WasteDisposalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteDisposal4")
  private val  WasteRecoveryLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wasteRecovery4")
  private val  WaterLvl2Form: Form[FormValues] = formWithSingleMandatoryField("water2")

  def loadAccommodationFoodLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AccommodationFoodLvl2Page(AccommodationFoodLvl2Form)).toFuture
  }

  def submitAccommodationFoodLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AccommodationFoodLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AccommodationFoodLvl2Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }


  def loadAccommodationLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AccommodationLvl3Page(AccommodationLvl3Form)).toFuture
  }

  def submitAccommodationLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AccommodationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AccommodationLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadEventCateringOtherFoodActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(EventCateringOtherFoodActivitiesLvl4Page(EventCateringOtherFoodActivitiesLvl4Form)).toFuture
  }

  def submitEventCateringOtherFoodActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EventCateringOtherFoodActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EventCateringOtherFoodActivitiesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadFoodBeverageActivitiesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FoodBeverageActivitiesLvl3Page(FoodBeverageActivitiesLvl3Form)).toFuture
  }

  def submitFoodBeverageActivitiesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FoodBeverageActivitiesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FoodBeverageActivitiesLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadRestaurantFoodServicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(RestaurantFoodServicesLvl4Page(RestaurantFoodServicesLvl4Form)).toFuture
  }

  def submitRestaurantFoodServicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RestaurantFoodServicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RestaurantFoodServicesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadElectricityLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ElectricityLvl3Page(ElectricityLvl3Form)).toFuture
  }

  def submitElectricityLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricityLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ElectricityLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadElectricityLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ElectricityLvl4Page(ElectricityLvl4Form)).toFuture
  }

  def submitElectricityLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricityLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ElectricityLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadGasManufactureLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(GasManufactureLvl4Page(GasManufactureLvl4Form)).toFuture
  }

  def submitGasManufactureLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    GasManufactureLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(GasManufactureLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWasteCollectionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WasteCollectionLvl4Page(WasteCollectionLvl4Form)).toFuture
  }

  def submitWasteCollectionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteCollectionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteCollectionLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWasteCollectionRecoveryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WasteCollectionRecoveryLvl3Page(WasteCollectionRecoveryLvl3Form)).toFuture
  }

  def submitWasteCollectionRecoveryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteCollectionRecoveryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteCollectionRecoveryLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWasteDisposalLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WasteDisposalLvl4Page(WasteDisposalLvl4Form)).toFuture
  }

  def submitWasteDisposalLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteDisposalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteDisposalLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWasteRecoveryLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WasteRecoveryLvl4Page(WasteRecoveryLvl4Form)).toFuture
  }

  def submitWasteRecoveryLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WasteRecoveryLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WasteRecoveryLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWaterLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WaterLvl2Page(WaterLvl2Form)).toFuture
  }

  def submitWaterLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WaterLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WaterLvl2Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
}