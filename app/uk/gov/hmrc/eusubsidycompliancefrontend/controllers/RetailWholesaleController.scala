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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.retailwholesale.RetailWholesaleLvl2Page
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.retailwholesale.retail.{CulturalLvl4Page, FoodLvl4Page, HouseholdLvl4Page, IntermediationLvl4Page, MotorVehiclesLvl4Page, NonSpecialisedLvl4Page, OtherGoodsLvl4Page, RetailLvl3Page}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.retailwholesale.wholesale.{AgriculturalLvl4Page, ContractBasisLvl4Page, FoodWholesaleLvl4Page, HouseholdWholesaleLvl4Page, MachineryLvl4Page, MotorVehiclesWholesaleLvl4Page, SpecialisedLvl4Page, WholesaleLvl3Page}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RetailWholesaleController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  retailWholesaleLvl2Page: RetailWholesaleLvl2Page,
  retailLvl3Page: RetailLvl3Page,
  culturalLvl4Page: CulturalLvl4Page,
  foodLvl4Page: FoodLvl4Page,
  householdLvl4Page: HouseholdLvl4Page,
  intermediationLvl4Page: IntermediationLvl4Page,
  motorVehiclesLvl4Page: MotorVehiclesLvl4Page,
  nonSpecialisedLvl4Page: NonSpecialisedLvl4Page,
  otherGoodsLvl4Page: OtherGoodsLvl4Page,
  wholesaleLvl3Page: WholesaleLvl3Page,
  agriculturalLvl4Page: AgriculturalLvl4Page,
  contractBasisLvl4Page: ContractBasisLvl4Page,
  foodWholesaleLvl4Page: FoodWholesaleLvl4Page,
  householdWholesaleLvl4Page: HouseholdWholesaleLvl4Page,
  machineryLvl4Page: MachineryLvl4Page,
  motorVehiclesWholesaleLvl4Page: MotorVehiclesWholesaleLvl4Page,
  specialisedLvl4Page: SpecialisedLvl4Page
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi
  private val retailWholesaleLvl2PageForm: Form[FormValues] = formWithSingleMandatoryField("retail2")
  private val retailLvl3PageForm: Form[FormValues] = formWithSingleMandatoryField("retail3")
  private val culturalLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("culturalRetail4")
  private val foodLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("foodRetail4")
  private val householdLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("householdRetail4")
  private val intermediationLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("intermediationRetail4")
  private val motorVehiclesLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("motorVehiclesRetail4")
  private val nonSpecialisedLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("nonSpecialRetail4")
  private val otherGoodsLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("otherRetail4")
  private val wholesaleLvl3PageForm: Form[FormValues] = formWithSingleMandatoryField("Wholesale3")
  private val agriculturalLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("agriWholesale4")
  private val contractBasisLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("contractWholesale4")
  private val foodWholesaleLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("foodWholesale4")
  private val householdWholesaleLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("householdWholesale4")
  private val machineryLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("machineryWholesale4")
  private val motorVehiclesWholesaleLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField(
    "motorVehiclesWholesale4"
  )
  private val specialisedLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("specialWholesale4")

  //retailWholesaleLvl2PageForm
  def loadRetailWholesaleLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(retailWholesaleLvl2Page(retailWholesaleLvl2PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitRetailWholesaleLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    retailWholesaleLvl2PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(retailWholesaleLvl2Page(formWithErrors, "")).toFuture,
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
            else if (previousAnswer.equals(form.value))
              if (journey.isAmend)
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }
  //retailLvl3PageForm
  def loadRetailLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(retailLvl3Page(retailLvl3PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitRetailLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    retailLvl3PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(retailLvl3Page(formWithErrors, "")).toFuture,
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
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }
  //culturalLvl4PageForm
  def loadCulturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(culturalLvl4Page(culturalLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitCulturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    culturalLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(culturalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //foodLvl4PageForm
  def loadFoodLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(foodLvl4Page(foodLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFoodLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //householdLvl4PageForm
  def loadHouseholdLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(householdLvl4Page(householdLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHouseholdLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    householdLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(householdLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //intermediationLvl4PageForm
  def loadIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(intermediationLvl4Page(intermediationLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    intermediationLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(intermediationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //motorVehiclesLvl4PageForm
  def loadMotorVehiclesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(motorVehiclesLvl4Page(motorVehiclesLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMotorVehiclesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    motorVehiclesLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(motorVehiclesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //nonSpecialisedLvl4PageForm
  def loadNonSpecialisedLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(nonSpecialisedLvl4Page(nonSpecialisedLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitNonSpecialisedLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    nonSpecialisedLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(nonSpecialisedLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //otherGoodsLvl4PageForm
  def loadOtherGoodsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(otherGoodsLvl4Page(otherGoodsLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherGoodsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherGoodsLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherGoodsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //wholesaleLvl3PageForm
  def loadWholesaleLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(wholesaleLvl3Page(wholesaleLvl3PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWholesaleLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    wholesaleLvl3PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(wholesaleLvl3Page(formWithErrors, "")).toFuture,
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
                Redirect(navigator.nextPage(form.value, appConfig.AmendNaceMode)).toFuture
              else
                Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            else {
              for {
                updatedSector <- store
                  .update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              } yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  //agriculturalLvl4PageForm
  def loadAgriculturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(agriculturalLvl4Page(agriculturalLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAgriculturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    agriculturalLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(agriculturalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //contractBasisLvl4PageForm
  def loadContractBasisLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(contractBasisLvl4Page(contractBasisLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitContractBasisLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    contractBasisLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(contractBasisLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //foodWholesaleLvl4PageForm
  def loadFoodWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(foodWholesaleLvl4Page(foodWholesaleLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFoodWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    foodWholesaleLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(foodWholesaleLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //householdWholesaleLvl4PageForm
  def loadHouseholdWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(householdWholesaleLvl4Page(householdWholesaleLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHouseholdWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    householdWholesaleLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(householdWholesaleLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //machineryLvl4PageForm
  def loadMachineryLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(machineryLvl4Page(machineryLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMachineryLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    machineryLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(machineryLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //motorVehiclesWholesaleLvl4PageForm
  def loadMotorVehiclesWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        motorVehiclesWholesaleLvl4Page(motorVehiclesWholesaleLvl4PageForm.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitMotorVehiclesWholesaleLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    motorVehiclesWholesaleLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(motorVehiclesWholesaleLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  //specialisedLvl4PageForm
  def loadSpecialisedLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(specialisedLvl4Page(specialisedLvl4PageForm.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitSpecialisedLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    specialisedLvl4PageForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(specialisedLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
