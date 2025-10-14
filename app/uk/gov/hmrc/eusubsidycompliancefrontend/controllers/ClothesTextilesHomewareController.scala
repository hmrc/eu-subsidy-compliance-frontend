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
  def loadClothingLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(clothingLvl3Page(clothingLvl3Form, isUpdate)).toFuture
  }

  def submitClothingLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clothingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothingLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //leatherLvl3Page
  def loadLeatherLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(leatherLvl3Page(leatherLvl3Form, isUpdate)).toFuture
  }

  def submitLeatherLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    leatherLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(leatherLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //RubberPlasticLvl3Page
  def loadRubberPlasticLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(rubberPlasticLvl3Page(rubberPlasticLvl3Form, isUpdate)).toFuture
  }

  def submitRubberPlasticLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    rubberPlasticLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberPlasticLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //textilesLvl3Page
  def loadTextilesLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(textilesLvl3Page(textilesLvl3Form, isUpdate)).toFuture
  }

  def submitTextilesLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    textilesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(textilesLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //woodCorkStrawLvl3Page
  def loadWoodCorkStrawLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(woodCorkStrawLvl3Page(woodCorkStrawLvl3Form, isUpdate)).toFuture
  }

  def submitWoodCorkStrawLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    woodCorkStrawLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //manufactureOfTextilesLvl4Page
  def loadManufactureOfTextilesLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(manufactureOfTextilesLvl4Page(manufactureOfTextilesLvl4Form, isUpdate)).toFuture
  }

  def submitManufactureOfTextilesLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    manufactureOfTextilesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(manufactureOfTextilesLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //otherClothingLvl4Page
  def loadOtherClothingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherClothingLvl4Page(otherClothingLvl4Form, isUpdate)).toFuture
  }

  def submitOtherClothingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherClothingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherClothingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //plasticLvl4Page
  def loadPlasticLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(plasticLvl4Page(plasticLvl4Form, isUpdate)).toFuture
  }

  def submitPlasticLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    plasticLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(plasticLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //rubberLvl4Page
  def loadRubberLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(rubberLvl4Page(rubberLvl4Form, isUpdate)).toFuture
  }

  def submitRubberLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    rubberLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //sawmillingWoodworkLvl4Page
  def loadSawmillingWoodworkLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(sawmillingWoodworkLvl4Page(sawmillingWoodworkLvl4Form, isUpdate)).toFuture
  }

  def submitSawmillingWoodworkLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    sawmillingWoodworkLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(sawmillingWoodworkLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //tanningDressingDyeingLvl4Page
  def loadTanningDressingDyeingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(tanningDressingDyeingLvl4Page(tanningDressingDyeingLvl4Form, isUpdate)).toFuture
  }

  def submitTanningDressingDyeingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    tanningDressingDyeingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(tanningDressingDyeingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  //woodCorkStrawPlaitingLvl4Page
  def loadWoodCorkStrawPlaitingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(woodCorkStrawPlaitingLvl4Page(woodCorkStrawPlaitingLvl4Form, isUpdate)).toFuture
  }

  def submitWoodCorkStrawPlaitingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    woodCorkStrawPlaitingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawPlaitingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }


}
