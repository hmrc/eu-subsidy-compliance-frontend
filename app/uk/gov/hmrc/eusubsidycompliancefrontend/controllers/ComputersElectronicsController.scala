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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.computersElectronics.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ComputersElectronicsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  componentsBoardsLvl4Page: ComponentsBoardsLvl4Page,
  computersElectronicsOpticalLvl3Page: ComputersElectronicsOpticalLvl3Page,
  domesticAppliancesLvl4Page: DomesticAppliancesLvl4Page,
  electricalEquipmentLvl3Page: ElectricalEquipmentLvl3Page,
  generalPurposeLvl4Page: GeneralPurposeLvl4Page,
  measuringTestingInstrumentsLvl4Page: MeasuringTestingInstrumentsLvl4Page,
  metalFormingLvl4Page: MetalFormingLvl4Page,
  motorsGeneratorsLvl4Page: MotorsGeneratorsLvl4Page,
  otherGeneralPurposeLvl4Page: OtherGeneralPurposeLvl4Page,
  otherMachineryLvl3Page: OtherMachineryLvl3Page,
  otherSpecialPurposeLvl4Page: OtherSpecialPurposeLvl4Page,
  repairMaintenanceLvl4Page: RepairMaintenanceLvl4Page,
  repairsMaintainInstallLvl3Page: RepairsMaintainInstallLvl3Page,
  wiringAndDevicesLvl4Page: WiringAndDevicesLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders.*
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val ComponentsBoardsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("boards4")
  private val ComputersElectronicsOpticalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("optical3")
  private val DomesticAppliancesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("domestic4")
  private val ElectricalEquipmentLvl3Form: Form[FormValues] = formWithSingleMandatoryField("electicEquip3")
  private val GeneralPurposeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("generalMachines4")
  private val MeasuringTestingInstrumentsLvl4Form: Form[FormValues] = formWithSingleMandatoryField(
    "testingInstruments4"
  )
  private val MetalFormingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("metalForming4")
  private val MotorsGeneratorsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("generators4")
  private val OtherGeneralPurposeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherGeneralPurpose4")
  private val OtherMachineryLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherMachines3")
  private val OtherSpecialPurposeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("specialMachines4")
  private val RepairMaintenanceLvl4Form: Form[FormValues] = formWithSingleMandatoryField("repair4")
  private val RepairsMaintainInstallLvl3Form: Form[FormValues] = formWithSingleMandatoryField("repair3")
  private val WiringAndDevicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wiring4")

  // ComponentsBoardsLvl4Page
  def loadComponentsBoardsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(componentsBoardsLvl4Page(ComponentsBoardsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitComponentsBoardsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComponentsBoardsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(componentsBoardsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // ComputersElectronicsOpticalLvl3Page
  def loadComputersElectronicsOpticalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        computersElectronicsOpticalLvl3Page(ComputersElectronicsOpticalLvl3Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitComputersElectronicsOpticalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputersElectronicsOpticalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(computersElectronicsOpticalLvl3Page(formWithErrors, "")).toFuture,
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

  // DomesticAppliancesLvl4Page
  def loadDomesticAppliancesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(domesticAppliancesLvl4Page(DomesticAppliancesLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitDomesticAppliancesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    DomesticAppliancesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(domesticAppliancesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // ElectricalEquipmentLvl3Page
  def loadElectricalEquipmentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(electricalEquipmentLvl3Page(ElectricalEquipmentLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitElectricalEquipmentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricalEquipmentLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(electricalEquipmentLvl3Page(formWithErrors, "")).toFuture,
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

  // GeneralPurposeLvl4Page
  def loadGeneralPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(generalPurposeLvl4Page(GeneralPurposeLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitGeneralPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    GeneralPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(generalPurposeLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // MeasuringTestingInstrumentsLvl4Page
  def loadMeasuringTestingInstrumentsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        measuringTestingInstrumentsLvl4Page(MeasuringTestingInstrumentsLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitMeasuringTestingInstrumentsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MeasuringTestingInstrumentsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(measuringTestingInstrumentsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // MetalFormingLvl4Page
  def loadMetalFormingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(metalFormingLvl4Page(MetalFormingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMetalFormingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MetalFormingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(metalFormingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // MotorsGeneratorsLvl4Page
  def loadMotorsGeneratorsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(motorsGeneratorsLvl4Page(MotorsGeneratorsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMotorsGeneratorsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MotorsGeneratorsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(motorsGeneratorsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // OtherGeneralPurposeLvl4Page
  def loadOtherGeneralPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(otherGeneralPurposeLvl4Page(OtherGeneralPurposeLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherGeneralPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherGeneralPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherGeneralPurposeLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // OtherMachineryLvl3Page
  def loadOtherMachineryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(otherMachineryLvl3Page(OtherMachineryLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherMachineryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherMachineryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherMachineryLvl3Page(formWithErrors, "")).toFuture,
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

  // OtherSpecialPurposeLvl4Page
  def loadOtherSpecialPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(otherSpecialPurposeLvl4Page(OtherSpecialPurposeLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherSpecialPurposeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherSpecialPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherSpecialPurposeLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // RepairMaintenanceLvl4Page
  def loadRepairMaintenanceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(repairMaintenanceLvl4Page(RepairMaintenanceLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitRepairMaintenanceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RepairMaintenanceLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(repairMaintenanceLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  // RepairsMaintainInstallLvl3Page
  def loadRepairsMaintainInstallLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(repairsMaintainInstallLvl3Page(RepairsMaintainInstallLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitRepairsMaintainInstallLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RepairsMaintainInstallLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(repairsMaintainInstallLvl3Page(formWithErrors, "")).toFuture,
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

  // WiringAndDevicesLvl4Page
  def loadWiringAndDevicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(wiringAndDevicesLvl4Page(WiringAndDevicesLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWiringAndDevicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WiringAndDevicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(wiringAndDevicesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val sectorEnum = Sector.fromCode(form.value)
          store
            .update[UndertakingJourney](_.setUndertakingSector(sectorEnum))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }
}
