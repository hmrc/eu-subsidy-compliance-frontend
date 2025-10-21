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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.agriculture._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.forestry._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.fishing._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AgricultureController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AgricultureLvl3Page: AgricultureLvl3Page,
  AnimalProductionLvl4Page: AnimalProductionLvl4Page,
  NonPerennialCropLvl4Page: NonPerennialCropLvl4Page,
  PerennialCropLvl4Page: PerennialCropLvl4Page,
  SupportActivitiesLvl4Page: SupportActivitiesLvl4Page,
  ForestryLvl3Page: ForestryLvl3Page,
  FishingAndAquacultureLvl3Page: FishingAndAquacultureLvl3Page,
  AquacultureLvl4Page: AquacultureLvl4Page,
  FishingLvl4Page: FishingLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val SupportActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("agriSupport4")
  private val PerennialCropLvl4Form: Form[FormValues] = formWithSingleMandatoryField("pCrops4")
  private val NonPerennialCropLvl4Form: Form[FormValues] = formWithSingleMandatoryField("nonPCrops4")
  private val AnimalProductionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("animal4")
  private val AgricultureLvl3Form: Form[FormValues] = formWithSingleMandatoryField("agriculture3")
  private val ForestryLvl3Form: Form[FormValues] = formWithSingleMandatoryField("forestry3")
  private val FishingAndAquacultureLvl3Form: Form[FormValues] = formWithSingleMandatoryField("fishing3")
  private val AquacultureLvl4Form: Form[FormValues] = formWithSingleMandatoryField("aquaculture4")
  private val FishingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("fishing4")

  def loadAgricultureLvl3Page( ): Action[AnyContent] = enrolled.async { implicit request =>
      implicit val eori: EORI = request.eoriNumber
      store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
        val sector = journey.sector.value match {
          case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
          case None => ""
        }
      Ok(AgricultureLvl3Page(AgricultureLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
      }
  }

  def submitAgricultureLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AgricultureLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AgricultureLvl3Page(formWithErrors, "")).toFuture,
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

  def loadSupportActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") SupportActivitiesLvl4Form else SupportActivitiesLvl4Form.fill(FormValues(sector))
      Ok(SupportActivitiesLvl4Page(form,  journey.mode)).toFuture
    }}

  def submitSupportActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    SupportActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SupportActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  def loadAnimalProductionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") AnimalProductionLvl4Form else AnimalProductionLvl4Form.fill(FormValues(sector))
      Ok(AnimalProductionLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitAnimalProductionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    AnimalProductionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AnimalProductionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  def loadPerennialCropLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") PerennialCropLvl4Form else PerennialCropLvl4Form.fill(FormValues(sector))
      Ok(PerennialCropLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitPerennialCropLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    PerennialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PerennialCropLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  def loadNonPerennialCropLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") NonPerennialCropLvl4Form else NonPerennialCropLvl4Form.fill(FormValues(sector))
      Ok(NonPerennialCropLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitNonPerennialCropLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    NonPerennialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NonPerennialCropLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  def loadForestryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") ForestryLvl3Form else ForestryLvl3Form.fill(FormValues(sector))
      Ok(ForestryLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitForestryLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ForestryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ForestryLvl3Page(formWithErrors, "")).toFuture,
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

  def loadFishingAndAquacultureLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form = if (sector == "") FishingAndAquacultureLvl3Form else FishingAndAquacultureLvl3Form.fill(FormValues(sector))
      Ok(FishingAndAquacultureLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitFishingAndAquacultureLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FishingAndAquacultureLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FishingAndAquacultureLvl3Page(formWithErrors, "")).toFuture,
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

  def loadAquacultureLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") AquacultureLvl4Form else AquacultureLvl4Form.fill(FormValues(sector))
      Ok(AquacultureLvl4Page(form, journey.mode)).toFuture
    }}

  def submitAquacultureLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    AquacultureLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AquacultureLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
            level3Display = formData.get("level3Display").flatMap(_.headOption),
            level4Display = formData.get(s"level4Display_${form.value}").flatMap(_.headOption).getOrElse("")
          )

          val sectorEnum = Sector.withName(form.value)

          store.update[UndertakingJourney](_.setNaceSelection(naceSelection).setUndertakingSector(sectorEnum.id))
            .flatMap(_ => Redirect(navigator.nextPage(form.value, "")).toFuture)
        }
      )
  }

  def loadFishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
      implicit val eori: EORI = request.eoriNumber
      store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
        val sector = journey.sector.value match {
          case Some(value) => value.toString
          case None => ""
        }
        Ok(FishingLvl4Page(FishingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
      }}

  def submitFishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    FishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FishingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

          val naceSelection = NaceSelection(
            code = form.value,
            sectorDisplay = formData.get("sectorDisplay").flatMap(_.headOption).getOrElse(""),
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
