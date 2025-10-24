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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.vehiclesTransport._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VehiclesManuTransportController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AircraftSpacecraftLvl4Page: AircraftSpacecraftLvl4Page,
  MotorVehiclesLvl3Page: MotorVehiclesLvl3Page,
  OtherTransportEquipmentLvl3Page: OtherTransportEquipmentLvl3Page,
  OtherTransportEquipmentLvl4Page: OtherTransportEquipmentLvl4Page,
  PartsAccessoriesLvl4Page: PartsAccessoriesLvl4Page,
  ShipsBoatsLvl4Page: ShipsBoatsLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AircraftSpacecraftLvl4Form: Form[FormValues] = formWithSingleMandatoryField("aircraft4")
  private val MotorVehiclesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("vehiclesMan3")
  private val OtherTransportEquipmentLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherTransport3")
  private val OtherTransportEquipmentLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherTransport4")
  private val PartsAccessoriesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("parts4")
  private val ShipsBoatsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("ships4")

  def loadAircraftSpacecraftLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(AircraftSpacecraftLvl4Page(AircraftSpacecraftLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAircraftSpacecraftLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AircraftSpacecraftLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AircraftSpacecraftLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadMotorVehiclesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(MotorVehiclesLvl3Page(MotorVehiclesLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMotorVehiclesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MotorVehiclesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MotorVehiclesLvl3Page(formWithErrors, "")).toFuture,
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

  def loadOtherTransportEquipmentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        OtherTransportEquipmentLvl3Page(OtherTransportEquipmentLvl3Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitOtherTransportEquipmentLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherTransportEquipmentLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherTransportEquipmentLvl3Page(formWithErrors, "")).toFuture,
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

  def loadOtherTransportEquipmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        OtherTransportEquipmentLvl4Page(OtherTransportEquipmentLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitOtherTransportEquipmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherTransportEquipmentLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherTransportEquipmentLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadPartsAccessoriesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(PartsAccessoriesLvl4Page(PartsAccessoriesLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPartsAccessoriesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PartsAccessoriesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PartsAccessoriesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadShipsBoatsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(ShipsBoatsLvl4Page(ShipsBoatsLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitShipsBoatsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ShipsBoatsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ShipsBoatsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
}
