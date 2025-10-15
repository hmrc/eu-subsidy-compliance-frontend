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

class GeneralTradeGroupsController @Inject()(
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
                                             vehiclesTransportPage: VehiclesTransportPage
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

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

  //GeneralTradeUndertakingPage
  def loadGeneralTradeUndertakingPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(generalTradeUndertakingPage(generalTradeUndertakingForm, mode)).toFuture
  }

  def submitGeneralTradeUndertakingPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //GeneralTradeUndertakingOtherPage
  def loadGeneralTradeUndertakingOtherPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(generalTradeUndertakingOtherPage(generalTradeUndertakingOtherForm, mode)).toFuture
  }

  def submitGeneralTradeUndertakingOtherPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingOtherForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingOtherPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //Lvl2_1GroupsPage
  def loadLvl2_1GroupsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(lvl2_1GroupsPage(lvl2_1GroupsForm, mode)).toFuture
  }

  def submitLvl2_1GroupsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    lvl2_1GroupsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(lvl2_1GroupsPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //ClothesTextilesHomewarePage
  def loadClothesTextilesHomewarePage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(clothesTextilesHomewarePage(clothesTextilesHomewareForm, mode)).toFuture
  }

  def submitClothesTextilesHomewarePage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clothesTextilesHomewareForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothesTextilesHomewarePage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //ComputersElectronicsMachineryPage
  def loadComputersElectronicsMachineryPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(computersElectronicsMachineryPage(computersElectronicsMachineryForm, mode)).toFuture
  }

  def submitComputersElectronicsMachineryPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    computersElectronicsMachineryForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(computersElectronicsMachineryPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //FoodBeveragesTobaccoPage
  def loadFoodBeveragesTobaccoPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(foodBeveragesTobaccoPage(foodBeveragesTobaccoForm, mode)).toFuture
  }

  def submitFoodBeveragesTobaccoPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodBeveragesTobaccoForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodBeveragesTobaccoPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //MetalsChemicalsMaterialsPage
  def loadMetalsChemicalsMaterialsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(metalsChemicalsMaterialsPage(metalsChemicalsMaterialsForm, mode)).toFuture
  }

  def submitMetalsChemicalsMaterialsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    metalsChemicalsMaterialsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(metalsChemicalsMaterialsPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //PaperPrintedProductsPage
  def loadPaperPrintedProductsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(paperPrintedProductsPage(paperPrintedProductsForm, mode)).toFuture
  }

  def submitPaperPrintedProductsPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    paperPrintedProductsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(paperPrintedProductsPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  //VehiclesTransportPage
  def loadVehiclesTransportPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(vehiclesTransportPage(vehiclesTransportForm, mode)).toFuture
  }

  def submitVehiclesTransportPage(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    vehiclesTransportForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(vehiclesTransportPage(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
}