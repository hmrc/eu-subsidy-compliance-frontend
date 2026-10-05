/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.MetalsChemicals.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MetalsChemicalsController @Inject() (
                                            mcc: MessagesControllerComponents,
                                            actionBuilders: ActionBuilders,
                                            val store: Store,
                                            navigator: Navigator,
                                            basicLvl4Page: BasicLvl4Page,
                                            basicMetalsLvl3Page: BasicMetalsLvl3Page,
                                            castingMetalsLvl4Page: CastingMetalsLvl4Page,
                                            chemicalsProductsLvl3Page: ChemicalsProductsLvl3Page,
                                            cokePetroleumLvl3Page: CokePetroleumLvl3Page,
                                            cutleryToolsHardwareLvl4Page: CutleryToolsHardwareLvl4Page,
                                            fabricatedMetalsLvl3Page: FabricatedMetalsLvl3Page,
                                            firstProcessingSteelLvl4Page: FirstProcessingSteelLvl4Page,
                                            otherFabricatedProductsLvl4Page: OtherFabricatedProductsLvl4Page,
                                            otherProductsLvl4Page: OtherProductsLvl4Page,
                                            pharmaceuticalsLvl3Page: PharmaceuticalsLvl3Page,
                                            preciousNonFerrousLvl4Page: PreciousNonFerrousLvl4Page,
                                            structuralMetalLvl4Page: StructuralMetalLvl4Page,
                                            tanksReservoirsContainersLvl4Page: TanksReservoirsContainersLvl4Page,
                                            treatmentCoatingMachiningLvl4Page: TreatmentCoatingMachiningLvl4Page,
                                            washingLvl4Page: WashingLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders.*
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
      Ok(pharmaceuticalsLvl3Page(PharmaceuticalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPharmaceuticalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PharmaceuticalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(pharmaceuticalsLvl3Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
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
      Ok(preciousNonFerrousLvl4Page(PreciousNonFerrousLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPreciousNonFerrousLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PreciousNonFerrousLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(preciousNonFerrousLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(structuralMetalLvl4Page(StructuralMetalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitStructuralMetalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    StructuralMetalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(structuralMetalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
        tanksReservoirsContainersLvl4Page(TanksReservoirsContainersLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitTanksReservoirsContainersLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TanksReservoirsContainersLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(tanksReservoirsContainersLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
        treatmentCoatingMachiningLvl4Page(TreatmentCoatingMachiningLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitTreatmentCoatingMachiningLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TreatmentCoatingMachiningLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(treatmentCoatingMachiningLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(washingLvl4Page(WashingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWashingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WashingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(washingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(basicLvl4Page(BasicLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitBasicLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BasicLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(basicLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(basicMetalsLvl3Page(BasicMetalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitBasicMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BasicMetalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(basicMetalsLvl3Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
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
      Ok(castingMetalsLvl4Page(CastingMetalsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCastingMetalsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CastingMetalsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(castingMetalsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(chemicalsProductsLvl3Page(ChemicalsProductsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitChemicalsProductsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ChemicalsProductsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(chemicalsProductsLvl3Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
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
      Ok(cokePetroleumLvl3Page(CokePetroleumLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCokePetroleumLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CokePetroleumLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(cokePetroleumLvl3Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
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
      Ok(cutleryToolsHardwareLvl4Page(CutleryToolsHardwareLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCutleryToolsHardwareLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CutleryToolsHardwareLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(cutleryToolsHardwareLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(fabricatedMetalsLvl3Page(FabricatedMetalsLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFabricatedMetalsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FabricatedMetalsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(fabricatedMetalsLvl3Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
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
      Ok(firstProcessingSteelLvl4Page(FirstProcessingSteelLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFirstProcessingSteelLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FirstProcessingSteelLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(firstProcessingSteelLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
        otherFabricatedProductsLvl4Page(OtherFabricatedProductsLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitOtherFabricatedProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherFabricatedProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherFabricatedProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
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
      Ok(otherProductsLvl4Page(OtherProductsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherProductsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProductsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherProductsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
