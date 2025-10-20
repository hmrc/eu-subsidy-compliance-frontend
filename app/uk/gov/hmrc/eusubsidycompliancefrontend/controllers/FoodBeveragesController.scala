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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.foodBeverages._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FoodBeveragesController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  foodLvl3Page: FoodLvl3Page,
  animalFeedsLvl4Page: AnimalFeedsLvl4Page,
  bakeryAndFarinaceousLvl4Page: BakeryAndFarinaceousLvl4Page,
  dairyProductsLvl4Page: DairyProductsLvl4Page,
  fruitAndVegLvl4Page: FruitAndVegLvl4Page,
  grainAndStarchLvl4Page: GrainAndStarchLvl4Page,
  meatLvl4Page: MeatLvl4Page,
  oilsAndFatsLvl4Page: OilsAndFatsLvl4Page,
  otherFoodProductsLvl4Page: OtherFoodProductsLvl4Page,
  beveragesLvl4Page: BeveragesLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val foodLvl3Form: Form[FormValues] = formWithSingleMandatoryField("food3")
  private val animalFeedsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("animalFood4")
  private val bakeryAndFarinaceousLvl4Form: Form[FormValues] = formWithSingleMandatoryField("bakery4")
  private val dairyProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("dairyFood4")
  private val fruitAndVegLvl4Form: Form[FormValues] = formWithSingleMandatoryField("fruit4")
  private val grainAndStarchLvl4Form: Form[FormValues] = formWithSingleMandatoryField("grain4")
  private val meatLvl4Form: Form[FormValues] = formWithSingleMandatoryField("meat4")
  private val oilsAndFatsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("oils4")
  private val otherFoodProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherFood4")
  private val beveragesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("beverages4")

  //FoodLvl3Page
  def loadFoodLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(foodLvl3Page(foodLvl3Form, "")).toFuture
  }

  def submitFoodLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  //AnimalFeedsLvl4Page
  def loadAnimalFeedsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(animalFeedsLvl4Page(animalFeedsLvl4Form, "")).toFuture
  }

  def submitAnimalFeedsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    animalFeedsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(animalFeedsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //BakeryAndFarinaceousLvl4Page
  def loadBakeryAndFarinaceousLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(bakeryAndFarinaceousLvl4Page(bakeryAndFarinaceousLvl4Form, "")).toFuture
  }

  def submitBakeryAndFarinaceousLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    bakeryAndFarinaceousLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(bakeryAndFarinaceousLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //DairyProductsLvl4Page
  def loadDairyProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(dairyProductsLvl4Page(dairyProductsLvl4Form, "")).toFuture
  }

  def submitDairyProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    dairyProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(dairyProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //FruitAndVegLvl4Page
  def loadFruitAndVegLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(fruitAndVegLvl4Page(fruitAndVegLvl4Form, "")).toFuture
  }

  def submitFruitAndVegLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    fruitAndVegLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(fruitAndVegLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //GrainAndStarchLvl4Page
  def loadGrainAndStarchLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(grainAndStarchLvl4Page(grainAndStarchLvl4Form, "")).toFuture
  }

  def submitGrainAndStarchLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    grainAndStarchLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(grainAndStarchLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //MeatLvl4Page
  def loadMeatLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(meatLvl4Page(meatLvl4Form, "")).toFuture
  }

  def submitMeatLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    meatLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(meatLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOilsAndFatsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(oilsAndFatsLvl4Page(oilsAndFatsLvl4Form, "")).toFuture
  }

  def submitOilsAndFatsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    oilsAndFatsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(oilsAndFatsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //OtherFoodProductsLvl4Page
  def loadOtherFoodProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherFoodProductsLvl4Page(otherFoodProductsLvl4Form, "")).toFuture
  }

  def submitOtherFoodProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherFoodProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherFoodProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadBeveragesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(beveragesLvl4Page(beveragesLvl4Form, "")).toFuture
  }

  def submitBeveragesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    beveragesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(beveragesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
