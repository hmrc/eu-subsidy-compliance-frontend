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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.clothesTextilesHomeware._

import javax.inject.Inject

class ClothesTextilesHomewareController @Inject()(mcc: MessagesControllerComponents,
                                                  actionBuilders: ActionBuilders,
                                                  val store: Store,
                                                  navigator: Navigator,
                                                  clothingLvl3Page: ClothingLvl3Page,
                                                  leatherLvl3Page: LeatherLvl3Page,
                                                  rubberPlasticLvl3Page: RubberPlasticLvl3Page,
                                                  textilesLvl3Page: TextilesLvl3Page,
                                                  woodCorkStrawLvl3Page: WoodCorkStrawLvl3Page,
                                                  manufactureOfTextilesLvl4Page: ManufactureOfTextilesLvl4Page,
                                                  otherClothingLvl4Page: OtherClothingLvl4Page,
                                                  plasticLvl4Page: PlasticLvl4Page,
                                                  rubberLvl4Page: RubberLvl4Page,
                                                  sawmillingWoodworkLvl4Page: SawmillingWoodworkLvl4Page,
                                                  tanningDressingDyeingLvl4Page: TanningDressingDyeingLvl4Page,
                                                  woodCorkStrawPlaitingLvl4Page: WoodCorkStrawPlaitingLvl4Page)
                                                 (implicit val appConfig: AppConfig) extends BaseController(mcc){
  import actionBuilders._
  private val clothingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("clothing3")
  private val leatherLvl3Form: Form[FormValues] = formWithSingleMandatoryField("leather3")
  private val rubberPlasticLvl3Form: Form[FormValues] = formWithSingleMandatoryField("rubber3")
  private val textilesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("textiles3")
  private val woodCorkStrawLvl3Form: Form[FormValues] = formWithSingleMandatoryField("straw3")
  private val manufactureOfTextilesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("textiles4")
  private val otherClothingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherClothing4")
  private val plasticLvl4Form: Form[FormValues] = formWithSingleMandatoryField("plastic4")
  private val rubberLvl4Form: Form[FormValues] = formWithSingleMandatoryField("rubber4")
  private val sawmillingWoodworkLvl4Form: Form[FormValues] = formWithSingleMandatoryField("sawmilling4")
  private val tanningDressingDyeingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("tanning4")
  private val woodCorkStrawPlaitingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("strawPlaiting4")

  //clothingLvl3Page
  def loadClothingLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(clothingLvl3Page(clothingLvl3Form)).toFuture
  }

  def submitClothingLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clothingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothingLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //leatherLvl3Page
  def loadLeatherLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(leatherLvl3Page(leatherLvl3Form)).toFuture
  }

  def submitLeatherLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    leatherLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(leatherLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //RubberPlasticLvl3Page
  def loadRubberPlasticLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(rubberPlasticLvl3Page(rubberPlasticLvl3Form)).toFuture
  }

  def submitRubberPlasticLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    rubberPlasticLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberPlasticLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //textilesLvl3Page
  def loadTextilesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(textilesLvl3Page(textilesLvl3Form)).toFuture
  }

  def submitTextilesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    textilesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(textilesLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //woodCorkStrawLvl3Page
  def loadWoodCorkStrawLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(woodCorkStrawLvl3Page(woodCorkStrawLvl3Form)).toFuture
  }

  def submitWoodCorkStrawLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    woodCorkStrawLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //manufactureOfTextilesLvl4Page
  def loadManufactureOfTextilesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(manufactureOfTextilesLvl4Page(manufactureOfTextilesLvl4Form)).toFuture
  }

  def submitManufactureOfTextilesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    manufactureOfTextilesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(manufactureOfTextilesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //otherClothingLvl4Page
  def loadOtherClothingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherClothingLvl4Page(otherClothingLvl4Form)).toFuture
  }

  def submitOtherClothingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherClothingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherClothingLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //plasticLvl4Page
  def loadPlasticLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(plasticLvl4Page(plasticLvl4Form)).toFuture
  }

  def submitPlasticLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    plasticLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(plasticLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //rubberLvl4Page
  def loadRubberLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(rubberLvl4Page(rubberLvl4Form)).toFuture
  }

  def submitRubberLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    rubberLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //sawmillingWoodworkLvl4Page
  def loadSawmillingWoodworkLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(sawmillingWoodworkLvl4Page(sawmillingWoodworkLvl4Form)).toFuture
  }

  def submitSawmillingWoodworkLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    sawmillingWoodworkLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(sawmillingWoodworkLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //tanningDressingDyeingLvl4Page
  def loadTanningDressingDyeingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(tanningDressingDyeingLvl4Page(tanningDressingDyeingLvl4Form)).toFuture
  }

  def submitTanningDressingDyeingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    tanningDressingDyeingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(tanningDressingDyeingLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //woodCorkStrawPlaitingLvl4Page
  def loadWoodCorkStrawPlaitingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(woodCorkStrawPlaitingLvl4Page(woodCorkStrawPlaitingLvl4Form)).toFuture
  }

  def submitWoodCorkStrawPlaitingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    woodCorkStrawPlaitingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawPlaitingLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }


}
