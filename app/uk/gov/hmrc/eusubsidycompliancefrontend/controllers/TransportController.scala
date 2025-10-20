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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.transport._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TransportController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AirTransportFreightAirLvl4Page: AirTransportFreightAirLvl4Page,
  AirTransportLvl3Page: AirTransportLvl3Page,
  LandTransportFreightTransportLvl4Page: LandTransportFreightTransportLvl4Page,
  LandTransportLvl3Page: LandTransportLvl3Page,
  LandTransportOtherPassengerLvl4Page: LandTransportOtherPassengerLvl4Page,
  LandTransportPassengerRailLvl4Page: LandTransportPassengerRailLvl4Page,
  PostalAndCourierLvl3Page: PostalAndCourierLvl3Page,
  TransportLvl2Page: TransportLvl2Page,
  WarehousingSupportActivitiesTransportLvl4Page: WarehousingSupportActivitiesTransportLvl4Page,
  WarehousingIntermediationLvl4Page: WarehousingIntermediationLvl4Page,
  WarehousingSupportLvl3Page: WarehousingSupportLvl3Page,
  WaterTransportLvl3Page: WaterTransportLvl3Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AirTransportFreightAirLvl4Form: Form[FormValues] = formWithSingleMandatoryField("airTransp4")
  private val AirTransportLvl3Form: Form[FormValues] = formWithSingleMandatoryField("airTransp3")
  private val LandTransportFreightTransportLvl4Form: Form[FormValues] = formWithSingleMandatoryField("freight4")
  private val LandTransportLvl3Form: Form[FormValues] = formWithSingleMandatoryField("landTransp3")
  private val LandTransportOtherPassengerLvl4Form: Form[FormValues] = formWithSingleMandatoryField("landOther4")
  private val LandTransportPassengerRailLvl4Form: Form[FormValues] = formWithSingleMandatoryField("landPassenger4")
  private val PostalAndCourierLvl3Form: Form[FormValues] = formWithSingleMandatoryField("postal3")
  private val TransportLvl2Form: Form[FormValues] = formWithSingleMandatoryField("transport2")
  private val WarehousingSupportActivitiesTransportLvl4Form: Form[FormValues] = formWithSingleMandatoryField(
    "wHouseSupport4"
  )
  private val WarehousingIntermediationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("wHouseInt4")
  private val WarehousingSupportLvl3Form: Form[FormValues] = formWithSingleMandatoryField("wHouse3")
  private val WaterTransportLvl3Form: Form[FormValues] = formWithSingleMandatoryField("waterTransp3")

  def loadAirTransportFreightAirLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AirTransportFreightAirLvl4Page(AirTransportFreightAirLvl4Form, "")).toFuture
  }

  def submitAirTransportFreightAirLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AirTransportFreightAirLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AirTransportFreightAirLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAirTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AirTransportLvl3Page(AirTransportLvl3Form, "")).toFuture
  }

  def submitAirTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AirTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AirTransportLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadLandTransportFreightTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LandTransportFreightTransportLvl4Page(LandTransportFreightTransportLvl4Form, "")).toFuture
  }

  def submitLandTransportFreightTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportFreightTransportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LandTransportFreightTransportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadLandTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LandTransportLvl3Page(LandTransportLvl3Form, "")).toFuture
  }

  def submitLandTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LandTransportLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadLandTransportOtherPassengerLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LandTransportOtherPassengerLvl4Page(LandTransportOtherPassengerLvl4Form, "")).toFuture
  }

  def submitLandTransportOtherPassengerLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportOtherPassengerLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LandTransportOtherPassengerLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadLandTransportPassengerRailLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LandTransportPassengerRailLvl4Page(LandTransportPassengerRailLvl4Form, "")).toFuture
  }

  def submitLandTransportPassengerRailLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportPassengerRailLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LandTransportPassengerRailLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadPostalAndCourierLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PostalAndCourierLvl3Page(PostalAndCourierLvl3Form, "")).toFuture
  }

  def submitPostalAndCourierLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PostalAndCourierLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PostalAndCourierLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadTransportLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(TransportLvl2Page(TransportLvl2Form, "")).toFuture
  }

  def submitTransportLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TransportLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TransportLvl2Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadWarehousingSupportActivitiesTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WarehousingSupportActivitiesTransportLvl4Page(WarehousingSupportActivitiesTransportLvl4Form, "")).toFuture
  }

  def submitWarehousingSupportActivitiesTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingSupportActivitiesTransportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WarehousingSupportActivitiesTransportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWarehousingIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WarehousingIntermediationLvl4Page(WarehousingIntermediationLvl4Form, "")).toFuture
  }

  def submitWarehousingIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingIntermediationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WarehousingIntermediationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWaterTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WaterTransportLvl3Page(WaterTransportLvl3Form, "")).toFuture
  }

  def submitWaterTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WaterTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WaterTransportLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadWarehousingSupportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WarehousingSupportLvl3Page(WarehousingSupportLvl3Form, "")).toFuture
  }

  def submitWarehousingSupportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingSupportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WarehousingSupportLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }
}
