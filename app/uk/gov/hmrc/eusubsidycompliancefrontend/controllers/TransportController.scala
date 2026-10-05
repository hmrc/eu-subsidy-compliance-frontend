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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.transport.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TransportController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  airTransportFreightAirLvl4Page: AirTransportFreightAirLvl4Page,
  airTransportLvl3Page: AirTransportLvl3Page,
  landTransportFreightTransportLvl4Page: LandTransportFreightTransportLvl4Page,
  landTransportLvl3Page: LandTransportLvl3Page,
  landTransportOtherPassengerLvl4Page: LandTransportOtherPassengerLvl4Page,
  landTransportPassengerRailLvl4Page: LandTransportPassengerRailLvl4Page,
  postalAndCourierLvl3Page: PostalAndCourierLvl3Page,
  transportLvl2Page: TransportLvl2Page,
  warehousingSupportActivitiesTransportLvl4Page: WarehousingSupportActivitiesTransportLvl4Page,
  warehousingIntermediationLvl4Page: WarehousingIntermediationLvl4Page,
  warehousingSupportLvl3Page: WarehousingSupportLvl3Page,
  waterTransportLvl3Page: WaterTransportLvl3Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders.*
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
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(airTransportFreightAirLvl4Page(AirTransportFreightAirLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAirTransportFreightAirLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AirTransportFreightAirLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(airTransportFreightAirLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAirTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(airTransportLvl3Page(AirTransportLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAirTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AirTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(airTransportLvl3Page(formWithErrors, "")).toFuture,
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

  def loadLandTransportFreightTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        landTransportFreightTransportLvl4Page(
          LandTransportFreightTransportLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitLandTransportFreightTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportFreightTransportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(landTransportFreightTransportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadLandTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(landTransportLvl3Page(LandTransportLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitLandTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(landTransportLvl3Page(formWithErrors, "")).toFuture,
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

  def loadLandTransportOtherPassengerLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        landTransportOtherPassengerLvl4Page(LandTransportOtherPassengerLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitLandTransportOtherPassengerLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportOtherPassengerLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(landTransportOtherPassengerLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadLandTransportPassengerRailLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        landTransportPassengerRailLvl4Page(LandTransportPassengerRailLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitLandTransportPassengerRailLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LandTransportPassengerRailLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(landTransportPassengerRailLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadPostalAndCourierLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(postalAndCourierLvl3Page(PostalAndCourierLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPostalAndCourierLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PostalAndCourierLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(postalAndCourierLvl3Page(formWithErrors, "")).toFuture,
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

  def loadTransportLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(transportLvl2Page(TransportLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitTransportLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TransportLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(transportLvl2Page(formWithErrors, "")).toFuture,
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

  def loadWarehousingSupportActivitiesTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        warehousingSupportActivitiesTransportLvl4Page(
          WarehousingSupportActivitiesTransportLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitWarehousingSupportActivitiesTransportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingSupportActivitiesTransportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(warehousingSupportActivitiesTransportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWarehousingIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        warehousingIntermediationLvl4Page(WarehousingIntermediationLvl4Form.fill(FormValues(sector)), journey.mode)
      ).toFuture
    }
  }

  def submitWarehousingIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingIntermediationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(warehousingIntermediationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.fromCode(form.value)))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadWaterTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(waterTransportLvl3Page(WaterTransportLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWaterTransportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WaterTransportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(waterTransportLvl3Page(formWithErrors, "")).toFuture,
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

  def loadWarehousingSupportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(warehousingSupportLvl3Page(WarehousingSupportLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWarehousingSupportLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WarehousingSupportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(warehousingSupportLvl3Page(formWithErrors, "")).toFuture,
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
}
