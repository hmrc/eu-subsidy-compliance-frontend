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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, NaceSelection}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.clothesTextilesHomeware._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ClothesTextilesHomewareController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  clothingLvl3Page: ClothingLvl3Page,
  leatherLvl3Page: LeatherLvl3Page,
  rubberPlasticLvl3Page: RubberPlasticLvl3Page,
  textilesLvl3Page: TextilesLvl3Page,
  woodCorkStrawLvl3Page: WoodCorkStrawLvl3Page,
  manufactureOfTextilesLvl4Page: ManufactureOfTextilesLvl4Page,
  otherClothingLvl4Page: OtherClothingLvl4Page,
  plasticLvl4Page: PlasticLvl4Page,
  rubberLvl4Page: RubberLvl4Page,
  sawmillingWoodworkLvl4Page: SawmillingWoodworkLvl4Page,
  tanningDressingDyeingLvl4Page: TanningDressingDyeingLvl4Page,
  woodCorkStrawPlaitingLvl4Page: WoodCorkStrawPlaitingLvl4Page
)(implicit val appConfig: AppConfig,
  val executionContext: ExecutionContext)
    extends BaseController(mcc) {
  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val clothingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("clothing3")
  private val leatherLvl3Form: Form[FormValues] = formWithSingleMandatoryField("leather3")
  private val rubberPlasticLvl3Form: Form[FormValues] = formWithSingleMandatoryField("rubber3")
  private val textilesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("textiles3")
  private val woodCorkStrawLvl3Form: Form[FormValues] = formWithSingleMandatoryField("straw3")
  private val manufactureOfTextilesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("textiles4")
  private val otherClothingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherClothing4")
  private val plasticLvl4Form: Form[FormValues] = formWithSingleMandatoryField("plastic4")
  private val rubberLvl4Form: Form[FormValues] = formWithSingleMandatoryField("rubber4")
  private val sawmillingWoodworkLvl4Form: Form[FormValues] = formWithSingleMandatoryField("sawmilling4")
  private val tanningDressingDyeingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("tanning4")
  private val woodCorkStrawPlaitingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("strawPlaiting4")

  def loadClothingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(clothingLvl3Page(clothingLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitClothingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    clothingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clothingLvl3Page(formWithErrors, "")).toFuture,
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

            val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

            val naceSelectionOpt = if (form.value == "14.10") {
              Some(NaceSelection(
                code = "14.10",
                sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
                level1Display = formData.get("level1Display").flatMap(_.headOption),
                level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
                level2Display = formData.get("level2Display").flatMap(_.headOption),
                level3Display = formData.get("level3Display_14.10").flatMap(_.headOption),
                level4Display = formData.get("level4Display_14.10").flatMap(_.headOption).getOrElse("")
              ))
            } else {
              None
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA) {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney](_.setNaceSelection(selection))
                    .flatMap(_ => Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture)
                case None =>
                  Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
              }
            } else {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney] { j =>
                    j.setNaceSelection(selection)
                      .setUndertakingSector(Sector.withName(form.value).id)
                      .copy(isNaceCYA = false)
                  }.flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
                case None =>
                  store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                    .flatMap(_ => store.update[UndertakingJourney](_.copy(isNaceCYA = false)))
                    .flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
              }
            }
          }
        }
      )
  }

  //leatherLvl3Page
  def loadLeatherLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(leatherLvl3Page(leatherLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitLeatherLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    leatherLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(leatherLvl3Page(formWithErrors, "")).toFuture,
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

            val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

            // Check if this is Footwear (auto-Level 4: 15.20)
            val naceSelectionOpt = if (form.value == "15.20") {
              Some(NaceSelection(
                code = "15.20",
                sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
                level1Display = formData.get("level1Display").flatMap(_.headOption),
                level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
                level2Display = formData.get("level2Display").flatMap(_.headOption),
                level3Display = formData.get("level3Display_15.20").flatMap(_.headOption),
                level4Display = formData.get("level4Display_15.20").flatMap(_.headOption).getOrElse("")
              ))
            } else {
              None
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA) {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney](_.setNaceSelection(selection))
                    .flatMap(_ => Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture)
                case None =>
                  Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
              }
            } else {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney] { j =>
                    j.setNaceSelection(selection)
                      .setUndertakingSector(Sector.withName(form.value).id)
                      .copy(isNaceCYA = false)
                  }.flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
                case None =>
                  store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                    .flatMap(_ => store.update[UndertakingJourney](_.copy(isNaceCYA = false)))
                    .flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
              }
            }
          }
        }
      )
  }

  //RubberPlasticLvl3Page
  def loadRubberPlasticLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(rubberPlasticLvl3Page(rubberPlasticLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitRubberPlasticLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    rubberPlasticLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberPlasticLvl3Page(formWithErrors, "")).toFuture,
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

  def loadTextilesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(textilesLvl3Page(textilesLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitTextilesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    textilesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(textilesLvl3Page(formWithErrors, "")).toFuture,
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

            val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

            val isAutoLevel4 = form.value == "13.10" || form.value == "13.20" || form.value == "13.30"

            val naceSelectionOpt = if (isAutoLevel4) {
              Some(NaceSelection(
                code = form.value,
                sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
                level1Display = formData.get("level1Display").flatMap(_.headOption),
                level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
                level2Display = formData.get("level2Display").flatMap(_.headOption),
                level3Display = formData.get(s"level3Display_${form.value}").flatMap(_.headOption),
                level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
              ))
            } else {
              None
            }

            if (previousAnswer.equals(form.value) && journey.isNaceCYA) {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney](_.setNaceSelection(selection))
                    .flatMap(_ => Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture)
                case None =>
                  Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
              }
            } else {
              naceSelectionOpt match {
                case Some(selection) =>
                  store.update[UndertakingJourney] { j =>
                    j.setNaceSelection(selection)
                      .setUndertakingSector(Sector.withName(form.value).id)
                      .copy(isNaceCYA = false)
                  }.flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
                case None =>
                  store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                    .flatMap(_ => store.update[UndertakingJourney](_.copy(isNaceCYA = false)))
                    .flatMap(_ => Redirect(navigator.nextPage(form.value, journey.mode)).toFuture)
              }
            }
          }
        }
      )
  }

  //woodCorkStrawLvl3Page
  def loadWoodCorkStrawLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(woodCorkStrawLvl3Page(woodCorkStrawLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitWoodCorkStrawLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    woodCorkStrawLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawLvl3Page(formWithErrors, "")).toFuture,
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

  //manufactureOfTextilesLvl4Page
  def loadManufactureOfTextilesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(manufactureOfTextilesLvl4Page(manufactureOfTextilesLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitManufactureOfTextilesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    manufactureOfTextilesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(manufactureOfTextilesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //otherClothingLvl4Page
  def loadOtherClothingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(otherClothingLvl4Page(otherClothingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitOtherClothingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    otherClothingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherClothingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //plasticLvl4Page
  def loadPlasticLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(plasticLvl4Page(plasticLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitPlasticLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    plasticLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(plasticLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //rubberLvl4Page
  def loadRubberLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(rubberLvl4Page(rubberLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitRubberLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    rubberLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(rubberLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //sawmillingWoodworkLvl4Page
  def loadSawmillingWoodworkLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(sawmillingWoodworkLvl4Page(sawmillingWoodworkLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitSawmillingWoodworkLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    sawmillingWoodworkLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(sawmillingWoodworkLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //tanningDressingDyeingLvl4Page
  def loadTanningDressingDyeingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(tanningDressingDyeingLvl4Page(tanningDressingDyeingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitTanningDressingDyeingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    tanningDressingDyeingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(tanningDressingDyeingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  //woodCorkStrawPlaitingLvl4Page
  def loadWoodCorkStrawPlaitingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(woodCorkStrawPlaitingLvl4Page(woodCorkStrawPlaitingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }}

  def submitWoodCorkStrawPlaitingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    woodCorkStrawPlaitingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(woodCorkStrawPlaitingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level1Display = formData.get("level1Display").flatMap(_.headOption),
            level1_1Display = formData.get("level1_1Display").flatMap(_.headOption),
            level2Display = formData.get("level2Display").flatMap(_.headOption),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

}
