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
  def loadGeneralTradeUndertakingPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(generalTradeUndertakingPage(generalTradeUndertakingForm, "")).toFuture
  }

  def submitGeneralTradeUndertakingPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //GeneralTradeUndertakingOtherPage
  def loadGeneralTradeUndertakingOtherPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(generalTradeUndertakingOtherPage(generalTradeUndertakingOtherForm, "")).toFuture
  }

  def submitGeneralTradeUndertakingOtherPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    generalTradeUndertakingOtherForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalTradeUndertakingOtherPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //Lvl2_1GroupsPage
  def loadLvl2_1GroupsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(lvl2_1GroupsPage(lvl2_1GroupsForm, "")).toFuture
  }

  def submitLvl2_1GroupsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    lvl2_1GroupsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(lvl2_1GroupsPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //ClothesTextilesHomewarePage
  def loadClothesTextilesHomewarePage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(clothesTextilesHomewarePage(clothesTextilesHomewareForm, "")).toFuture
  }

  def submitClothesTextilesHomewarePage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clothesTextilesHomewareForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothesTextilesHomewarePage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //ComputersElectronicsMachineryPage
  def loadComputersElectronicsMachineryPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(computersElectronicsMachineryPage(computersElectronicsMachineryForm, "")).toFuture
  }

  def submitComputersElectronicsMachineryPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    computersElectronicsMachineryForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(computersElectronicsMachineryPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //FoodBeveragesTobaccoPage
  def loadFoodBeveragesTobaccoPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(foodBeveragesTobaccoPage(foodBeveragesTobaccoForm, "")).toFuture
  }

  def submitFoodBeveragesTobaccoPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodBeveragesTobaccoForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodBeveragesTobaccoPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //MetalsChemicalsMaterialsPage
  def loadMetalsChemicalsMaterialsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(metalsChemicalsMaterialsPage(metalsChemicalsMaterialsForm, "")).toFuture
  }

  def submitMetalsChemicalsMaterialsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    metalsChemicalsMaterialsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(metalsChemicalsMaterialsPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //PaperPrintedProductsPage
  def loadPaperPrintedProductsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(paperPrintedProductsPage(paperPrintedProductsForm, "")).toFuture
  }

  def submitPaperPrintedProductsPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    paperPrintedProductsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(paperPrintedProductsPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //VehiclesTransportPage
  def loadVehiclesTransportPage() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(vehiclesTransportPage(vehiclesTransportForm, "")).toFuture
  }

  def submitVehiclesTransportPage() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    vehiclesTransportForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(vehiclesTransportPage(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
}