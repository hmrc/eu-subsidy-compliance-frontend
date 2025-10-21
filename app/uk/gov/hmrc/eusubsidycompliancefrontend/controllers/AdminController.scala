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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.administrative._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AdminController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AdministrativeLvl2Page: AdministrativeLvl2Page,
  BuildingsLvl3Page: BuildingsLvl3Page,
  CleaningLvl4Page: CleaningLvl4Page,
  EmploymentLvl3Page: EmploymentLvl3Page,
  IntermediationServicesLvl4Page: IntermediationServicesLvl4Page,
  InvestigationLvl4Page: InvestigationLvl4Page,
  MachineryEquipmentLvl4Page: MachineryEquipmentLvl4Page,
  MotorVehiclesLvl4Page: MotorVehiclesLvl4Page,
  OfficeLvl3Page: OfficeLvl3Page,
  OtherBusinessSupportLvl4Page: OtherBusinessSupportLvl4Page,
  PersonalHouseholdLvl4Page: PersonalHouseholdLvl4Page,
  RentalLvl3Page: RentalLvl3Page,
  TravelAgencyLvl4Page: TravelAgencyLvl4Page,
  TravelLvl3Page: TravelLvl3Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AdministrativeLvl2Form: Form[FormValues] = formWithSingleMandatoryField("admin2")
  private val BuildingsLvl3Form: Form[FormValues] = formWithSingleMandatoryField("building3")
  private val CleaningLvl4Form: Form[FormValues] = formWithSingleMandatoryField("cleaning3")
  private val EmploymentLvl3Form: Form[FormValues] = formWithSingleMandatoryField("employment3")
  private val IntermediationServicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("intermediation4")
  private val InvestigationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("investigation4")
  private val MachineryEquipmentLvl4Form: Form[FormValues] = formWithSingleMandatoryField("equipment4")
  private val MotorVehiclesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("vehicles4")
  private val OfficeLvl3Form: Form[FormValues] = formWithSingleMandatoryField("office3")
  private val OtherBusinessSupportLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherBusSupport4")
  private val PersonalHouseholdLvl4Form: Form[FormValues] = formWithSingleMandatoryField("personalHouse4")
  private val RentalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("rental3")
  private val TravelAgencyLvl4Form: Form[FormValues] = formWithSingleMandatoryField("travelAgency4")
  private val TravelLvl3Form: Form[FormValues] = formWithSingleMandatoryField("travel3")

  def loadTravelLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") TravelLvl3Form else TravelLvl3Form.fill(FormValues(sector))
      Ok(TravelLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitTravelLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TravelLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TravelLvl3Page(formWithErrors, "")).toFuture,
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

  def loadRentalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") RentalLvl3Form else RentalLvl3Form.fill(FormValues(sector))
      Ok(RentalLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitRentalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RentalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RentalLvl3Page(formWithErrors, "")).toFuture,
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

  def loadTravelAgencyLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") TravelAgencyLvl4Form else TravelAgencyLvl4Form.fill(FormValues(sector))
      Ok(TravelAgencyLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitTravelAgencyLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TravelAgencyLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TravelAgencyLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAdministrativeLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      val form = if (sector == "") AdministrativeLvl2Form else AdministrativeLvl2Form.fill(FormValues(sector))
      Ok(AdministrativeLvl2Page(form, journey.mode)).toFuture
    }
  }

  def submitAdministrativeLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AdministrativeLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AdministrativeLvl2Page(formWithErrors, "")).toFuture,
        form => {
          store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
            val previousAnswer = journey.sector.value match {
              case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
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

  def loadBuildingsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") BuildingsLvl3Form else BuildingsLvl3Form.fill(FormValues(sector))
      Ok(BuildingsLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitBuildingsLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BuildingsLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BuildingsLvl3Page(formWithErrors, "")).toFuture,
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

  def loadCleaningLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") CleaningLvl4Form else CleaningLvl4Form.fill(FormValues(sector))
      Ok(CleaningLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitCleaningLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CleaningLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(CleaningLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadEmploymentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") EmploymentLvl3Form else EmploymentLvl3Form.fill(FormValues(sector))
      Ok(EmploymentLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitEmploymentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EmploymentLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EmploymentLvl3Page(formWithErrors, "")).toFuture,
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

  def loadIntermediationServicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") IntermediationServicesLvl4Form else IntermediationServicesLvl4Form.fill(FormValues(sector))
      Ok(IntermediationServicesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitIntermediationServicesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    IntermediationServicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(IntermediationServicesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadInvestigationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") InvestigationLvl4Form else InvestigationLvl4Form.fill(FormValues(sector))
      Ok(InvestigationLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitInvestigationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    InvestigationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(InvestigationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadMachineryEquipmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") MachineryEquipmentLvl4Form else MachineryEquipmentLvl4Form.fill(FormValues(sector))
      Ok(MachineryEquipmentLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitMachineryEquipmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MachineryEquipmentLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MachineryEquipmentLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadMotorVehiclesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") MotorVehiclesLvl4Form else MotorVehiclesLvl4Form.fill(FormValues(sector))
      Ok(MotorVehiclesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitMotorVehiclesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MotorVehiclesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MotorVehiclesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOfficeLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") OfficeLvl3Form else OfficeLvl3Form.fill(FormValues(sector))
      Ok(OfficeLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitOfficeLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OfficeLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OfficeLvl3Page(formWithErrors, "")).toFuture,
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

  def loadOtherBusinessSupportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") OtherBusinessSupportLvl4Form else OtherBusinessSupportLvl4Form.fill(FormValues(sector))
      Ok(OtherBusinessSupportLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitOtherBusinessSupportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherBusinessSupportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherBusinessSupportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadPersonalHouseholdLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") PersonalHouseholdLvl4Form else PersonalHouseholdLvl4Form.fill(FormValues(sector))
      Ok(PersonalHouseholdLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitPersonalHouseholdLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PersonalHouseholdLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PersonalHouseholdLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
