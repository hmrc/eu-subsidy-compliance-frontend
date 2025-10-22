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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.MetalsChemicals._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MetalsChemicalsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  BasicLvl4Page: BasicLvl4Page,
  BasicMetalsLvl3Page: BasicMetalsLvl3Page,
  CastingMetalsLvl4Page: CastingMetalsLvl4Page,
  ChemicalsProductsLvl3Page: ChemicalsProductsLvl3Page,
  CokePetroleumLvl3Page: CokePetroleumLvl3Page,
  CutleryToolsHardwareLvl4Page: CutleryToolsHardwareLvl4Page,
  FabricatedMetalsLvl3Page: FabricatedMetalsLvl3Page,
  FirstProcessingSteelLvl4Page: FirstProcessingSteelLvl4Page,
  OtherFabricatedProductsLvl4Page: OtherFabricatedProductsLvl4Page,
  OtherProductsLvl4Page: OtherProductsLvl4Page,
  PharmaceuticalsLvl3Page: PharmaceuticalsLvl3Page,
  PreciousNonFerrousLvl4Page: PreciousNonFerrousLvl4Page,
  StructuralMetalLvl4Page: StructuralMetalLvl4Page,
  TanksReservoirsContainersLvl4Page: TanksReservoirsContainersLvl4Page,
  TreatmentCoatingMachiningLvl4Page: TreatmentCoatingMachiningLvl4Page,
  WashingLvl4Page: WashingLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val BasicLvl4Form: Form[FormValues] = formWithSingleMandatoryField("basicChem4")
  private val BasicMetalsLvl3Form: Form[FormValues] = formWithSingleMandatoryField("basicMetals3")
  private val CastingMetalsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("castingMetals4")
  private val ChemicalsProductsLvl3Form: Form[FormValues] = formWithSingleMandatoryField("chemProds3")
  private val CokePetroleumLvl3Form: Form[FormValues] = formWithSingleMandatoryField("coke3")
  private val CutleryToolsHardwareLvl4Form: Form[FormValues] = formWithSingleMandatoryField("cutlery4")
  private val FabricatedMetalsLvl3Form: Form[FormValues] = formWithSingleMandatoryField("fabMetal3")
  private val FirstProcessingSteelLvl4Form: Form[FormValues] = formWithSingleMandatoryField("steel4")
  private val OtherFabricatedProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherFab4")
  private val OtherProductsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherProducts4")
  private val PharmaceuticalsLvl3Form: Form[FormValues] = formWithSingleMandatoryField("pharm3")
  private val PreciousNonFerrousLvl4Form: Form[FormValues] = formWithSingleMandatoryField("preciousNonIron4")
  private val StructuralMetalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("structuralMetal4")
  private val TanksReservoirsContainersLvl4Form: Form[FormValues] = formWithSingleMandatoryField("tanks4")
  private val TreatmentCoatingMachiningLvl4Form: Form[FormValues] = formWithSingleMandatoryField("treatment4")
  private val WashingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("washing4")

  def loadPharmaceuticalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(PharmaceuticalsLvl3Page(PharmaceuticalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPharmaceuticalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PharmaceuticalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PharmaceuticalsLvl3Page(formWithErrors, "")).toFuture,
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
  def loadPreciousNonFerrousLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(PreciousNonFerrousLvl4Page(PreciousNonFerrousLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPreciousNonFerrousLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PreciousNonFerrousLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PreciousNonFerrousLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadStructuralMetalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(StructuralMetalLvl4Page(StructuralMetalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitStructuralMetalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    StructuralMetalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(StructuralMetalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadTanksReservoirsContainersLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        TanksReservoirsContainersLvl4Page(TanksReservoirsContainersLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitTanksReservoirsContainersLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TanksReservoirsContainersLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TanksReservoirsContainersLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadTreatmentCoatingMachiningLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        TreatmentCoatingMachiningLvl4Page(TreatmentCoatingMachiningLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitTreatmentCoatingMachiningLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TreatmentCoatingMachiningLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TreatmentCoatingMachiningLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadWashingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(WashingLvl4Page(WashingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWashingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WashingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WashingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadBasicLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(BasicLvl4Page(BasicLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitBasicLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BasicLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BasicLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadBasicMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(BasicMetalsLvl3Page(BasicMetalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitBasicMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BasicMetalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BasicMetalsLvl3Page(formWithErrors, "")).toFuture,
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

  def loadCastingMetalsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(CastingMetalsLvl4Page(CastingMetalsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCastingMetalsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CastingMetalsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(CastingMetalsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadChemicalsProductsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(ChemicalsProductsLvl3Page(ChemicalsProductsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitChemicalsProductsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ChemicalsProductsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ChemicalsProductsLvl3Page(formWithErrors, "")).toFuture,
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

  def loadCokePetroleumLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(CokePetroleumLvl3Page(CokePetroleumLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCokePetroleumLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CokePetroleumLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(CokePetroleumLvl3Page(formWithErrors, "")).toFuture,
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

  def loadCutleryToolsHardwareLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(CutleryToolsHardwareLvl4Page(CutleryToolsHardwareLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCutleryToolsHardwareLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CutleryToolsHardwareLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(CutleryToolsHardwareLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadFabricatedMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(FabricatedMetalsLvl3Page(FabricatedMetalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFabricatedMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FabricatedMetalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FabricatedMetalsLvl3Page(formWithErrors, "")).toFuture,
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

  def loadFirstProcessingSteelLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(FirstProcessingSteelLvl4Page(FirstProcessingSteelLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFirstProcessingSteelLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FirstProcessingSteelLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FirstProcessingSteelLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOtherFabricatedProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        OtherFabricatedProductsLvl4Page(OtherFabricatedProductsLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitOtherFabricatedProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherFabricatedProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherFabricatedProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOtherProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherProductsLvl4Page(OtherProductsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
