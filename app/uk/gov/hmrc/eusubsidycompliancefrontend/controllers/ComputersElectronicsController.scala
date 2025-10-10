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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.computersElectronics._

import javax.inject.Inject

class ComputersElectronicsController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             ComponentsBoardsLvl4Page: ComponentsBoardsLvl4Page,
                                             ComputersElectronicsOpticalLvl3Page: ComputersElectronicsOpticalLvl3Page,
                                             DomesticAppliancesLvl4Page: DomesticAppliancesLvl4Page,
                                             ElectricalEquipmentLvl3Page: ElectricalEquipmentLvl3Page,
                                             GeneralPurposeLvl4Page: GeneralPurposeLvl4Page,
                                             MeasuringTestingInstrumentsLvl4Page: MeasuringTestingInstrumentsLvl4Page,
                                             MetalFormingLvl4Page: MetalFormingLvl4Page,
                                             MotorsGeneratorsLvl4Page: MotorsGeneratorsLvl4Page,
                                             OtherGeneralPurposeLvl4Page: OtherGeneralPurposeLvl4Page,
                                             OtherMachineryLvl3Page: OtherMachineryLvl3Page,
                                             OtherSpecialPurposeLvl4Page: OtherSpecialPurposeLvl4Page,
                                             RepairMaintenanceLvl4Page: RepairMaintenanceLvl4Page,
                                             RepairsMaintainInstallLvl3Page: RepairsMaintainInstallLvl3Page,
                                             WiringAndDevicesLvl4Page: WiringAndDevicesLvl4Page,


                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val ComponentsBoardsLvl4Form : Form[FormValues] = formWithSingleMandatoryField("boards4")
  private val ComputersElectronicsOpticalLvl3Form : Form[FormValues] = formWithSingleMandatoryField("optical3")
  private val DomesticAppliancesLvl4Form : Form[FormValues] = formWithSingleMandatoryField("domestic4")
  private val ElectricalEquipmentLvl3Form: Form[FormValues] = formWithSingleMandatoryField("electicEquip3")
  private val GeneralPurposeLvl4Form : Form[FormValues] = formWithSingleMandatoryField("generalMachines4")
  private val MeasuringTestingInstrumentsLvl4Form : Form[FormValues] = formWithSingleMandatoryField("testingInstruments4")
  private val MetalFormingLvl4Form : Form[FormValues] = formWithSingleMandatoryField("metalForming4")
  private val MotorsGeneratorsLvl4Form : Form[FormValues] = formWithSingleMandatoryField("generators4")
  private val OtherGeneralPurposeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherGeneralPurpose4")
  private val OtherMachineryLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherMachines3")
  private val OtherSpecialPurposeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("specialMachines4")
  private val RepairMaintenanceLvl4Form: Form[FormValues] = formWithSingleMandatoryField("repair4")
  private val RepairsMaintainInstallLvl3Form: Form[FormValues] = formWithSingleMandatoryField("repair3")
  private val WiringAndDevicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wiring4")

  def loadOtherSpecialPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherSpecialPurposeLvl4Page(OtherSpecialPurposeLvl4Form)).toFuture
  }

  def submitOtherSpecialPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherSpecialPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherSpecialPurposeLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadRepairMaintenanceLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(RepairMaintenanceLvl4Page(RepairMaintenanceLvl4Form)).toFuture
  }

  def submitRepairMaintenanceLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RepairMaintenanceLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RepairMaintenanceLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadWiringAndDevicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WiringAndDevicesLvl4Page(WiringAndDevicesLvl4Form)).toFuture
  }

  def submitWiringAndDevicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WiringAndDevicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WiringAndDevicesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadRepairsMaintainInstallLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(RepairsMaintainInstallLvl3Page(RepairsMaintainInstallLvl3Form)).toFuture
  }

  def submitRepairsMaintainInstallLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RepairsMaintainInstallLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RepairsMaintainInstallLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
  def loadComponentsBoardsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ComponentsBoardsLvl4Page(ComponentsBoardsLvl4Form)).toFuture
  }

  def submitComponentsBoardsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComponentsBoardsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComponentsBoardsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }


  def loadComputersElectronicsOpticalLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ComputersElectronicsOpticalLvl3Page(ComputersElectronicsOpticalLvl3Form)).toFuture
  }

  def submitComputersElectronicsOpticalLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputersElectronicsOpticalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComputersElectronicsOpticalLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadDomesticAppliancesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(DomesticAppliancesLvl4Page(DomesticAppliancesLvl4Form)).toFuture
  }

  def submitDomesticAppliancesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    DomesticAppliancesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(DomesticAppliancesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadElectricalEquipmentLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ElectricalEquipmentLvl3Page(ElectricalEquipmentLvl3Form)).toFuture
  }

  def submitElectricalEquipmentLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ElectricalEquipmentLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ElectricalEquipmentLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadGeneralPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(GeneralPurposeLvl4Page(GeneralPurposeLvl4Form)).toFuture
  }

  def submitGeneralPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    GeneralPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(GeneralPurposeLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
  def loadMeasuringTestingInstrumentsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MeasuringTestingInstrumentsLvl4Page(MeasuringTestingInstrumentsLvl4Form)).toFuture
  }

  def submitMeasuringTestingInstrumentsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MeasuringTestingInstrumentsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MeasuringTestingInstrumentsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadMetalFormingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MetalFormingLvl4Page(MetalFormingLvl4Form)).toFuture
  }

  def submitMetalFormingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MetalFormingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MetalFormingLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadMotorsGeneratorsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MotorsGeneratorsLvl4Page(MotorsGeneratorsLvl4Form)).toFuture
  }

  def submitMotorsGeneratorsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MotorsGeneratorsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MotorsGeneratorsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadOtherGeneralPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherGeneralPurposeLvl4Page(OtherGeneralPurposeLvl4Form)).toFuture
  }

  def submitOtherGeneralPurposeLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherGeneralPurposeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherGeneralPurposeLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadOtherMachineryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherMachineryLvl3Page(OtherMachineryLvl3Form)).toFuture
  }

  def submitOtherMachineryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherMachineryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherMachineryLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
}