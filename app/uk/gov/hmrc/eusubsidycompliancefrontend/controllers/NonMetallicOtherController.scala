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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.nonMetallicOther._

import javax.inject.Inject

class NonMetallicOtherController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             nonMetallicMineralLvl3Page: NonMetallicMineralLvl3Page,
                                             otherManufacturingLvl3Page: OtherManufacturingLvl3Page,
                                             anotherTypeLvl4Page: AnotherTypeLvl4Page,
                                             cementLimePlasterLvl4Page: CementLimePlasterLvl4Page,
                                             clayBuildingMaterialsLvl4Page: ClayBuildingMaterialsLvl4Page,
                                             concreteCementPlasterLvl4Page: ConcreteCementPlasterLvl4Page,
                                             glassProductsLvl4Page: GlassProductsLvl4Page,
                                             jewelleryCoinsLvl4Page: JewelleryCoinsLvl4Page,
                                             otherPorcelainAndCeramicsLvl4Page: OtherPorcelainAndCeramicsLvl4Page,
                                             otherProductsLvl4Page: OtherProductsLvl4Page
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._

  private val nonMetallicMineralLvl3Form: Form[FormValues] = formWithSingleMandatoryField("mineral3")
  private val otherManufacturingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherManufacturing")
  private val anotherTypeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherMineral4")
  private val cementLimePlasterLvl4Form: Form[FormValues] = formWithSingleMandatoryField("cement4")
  private val clayBuildingMaterialsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("clay4")
  private val concreteCementPlasterLvl4Form: Form[FormValues] = formWithSingleMandatoryField("concrete4")
  private val glassProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("glass4")
  private val jewelleryCoinsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("jewellery4")
  private val otherPorcelainAndCeramicsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("porcelain4")
  private val otherProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherProducts4")


  //nonMetallicMineralLvl3Page
  def loadNonMetallicMineralLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(nonMetallicMineralLvl3Page(nonMetallicMineralLvl3Form)).toFuture
  }

  def submitNonMetallicMineralLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    nonMetallicMineralLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(nonMetallicMineralLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //OtherManufacturingLvl3Page
  def loadOtherManufacturingLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherManufacturingLvl3Page(otherManufacturingLvl3Form)).toFuture
  }

  def submitOtherManufacturingLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherManufacturingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherManufacturingLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //AnotherTypeLvl4Page
  def loadAnotherTypeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(anotherTypeLvl4Page(anotherTypeLvl4Form)).toFuture
  }

  def submitAnotherTypeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    anotherTypeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(anotherTypeLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //CementLimePlasterLvl4Page
  def loadCementLimePlasterLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(cementLimePlasterLvl4Page(cementLimePlasterLvl4Form)).toFuture
  }

  def submitCementLimePlasterLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cementLimePlasterLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(cementLimePlasterLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //ClayBuildingMaterialsLvl4Page
  def loadClayBuildingMaterialsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(clayBuildingMaterialsLvl4Page(clayBuildingMaterialsLvl4Form)).toFuture
  }

  def submitClayBuildingMaterialsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    clayBuildingMaterialsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clayBuildingMaterialsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //ConcreteCementPlasterLvl4Page
  def loadConcreteCementPlasterLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(concreteCementPlasterLvl4Page(concreteCementPlasterLvl4Form)).toFuture
  }

  def submitConcreteCementPlasterLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    concreteCementPlasterLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(concreteCementPlasterLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //GlassProductsLvl4Page
  def loadGlassProductsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(glassProductsLvl4Page(glassProductsLvl4Form)).toFuture
  }

  def submitGlassProductsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    glassProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(glassProductsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //JewelleryCoinsLvl4Page
  def loadJewelleryCoinsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(jewelleryCoinsLvl4Page(jewelleryCoinsLvl4Form)).toFuture
  }

  def submitJewelleryCoinsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    jewelleryCoinsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(jewelleryCoinsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }


  //OtherPorcelainAndCeramicsLvl4Page
  def loadOtherPorcelainAndCeramicsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherPorcelainAndCeramicsLvl4Page(otherPorcelainAndCeramicsLvl4Form)).toFuture
  }

  def submitOtherPorcelainAndCeramicsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherPorcelainAndCeramicsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherPorcelainAndCeramicsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //OtherProductsLvl4Page
  def loadOtherProductsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherProductsLvl4Page(otherProductsLvl4Form)).toFuture
  }

  def submitOtherProductsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherProductsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

}
