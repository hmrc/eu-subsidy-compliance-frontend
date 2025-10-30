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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.artSportsRecreation._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ArtsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  AmusementAndRecreationLvl4Page: AmusementAndRecreationLvl4Page,
  ArtsCreationLvl4Page: ArtsCreationLvl4Page,
  ArtsCreationPerformingLvl3Page: ArtsCreationPerformingLvl3Page,
  ArtsPerformingSupportActivitiesLvl4Page: ArtsPerformingSupportActivitiesLvl4Page,
  ArtsSportsRecreationLvl2Page: ArtsSportsRecreationLvl2Page,
  BotanicalZoologicalReservesLvl4Page: BotanicalZoologicalReservesLvl4Page,
  LibrariesArchivesCulturalLvl3Page: LibrariesArchivesCulturalLvl3Page,
  LibrariesArchivesLvl4Page: LibrariesArchivesLvl4Page,
  MuseumsCollectionsMomumentsLvl4Page: MuseumsCollectionsMomumentsLvl4Page,
  SportsAmusementRecreationLvl3Page: SportsAmusementRecreationLvl3Page,
  SportsLvl4Page: SportsLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AmusementAndRecreationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("amusement4")
  private val ArtsCreationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("artsCreation4")
  private val ArtsCreationPerformingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("artsCreation3")
  private val ArtsPerformingSupportActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField(
    "artsPerforming4"
  )
  private val ArtsSportsRecreationLvl2Form: Form[FormValues] = formWithSingleMandatoryField("artsSports2")
  private val BotanicalZoologicalReservesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("zoo4")
  private val LibrariesArchivesCulturalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("libraries3")
  private val LibrariesArchivesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("libraries4")
  private val MuseumsCollectionsMomumentsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("museums4")
  private val SportsAmusementRecreationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("sports3")
  private val SportsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("sports4")

  def loadAmusementAndRecreationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(AmusementAndRecreationLvl4Page(AmusementAndRecreationLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAmusementAndRecreationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AmusementAndRecreationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AmusementAndRecreationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadArtsCreationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") ArtsCreationLvl4Form else ArtsCreationLvl4Form.fill(FormValues(sector))
      Ok(ArtsCreationLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitArtsCreationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsCreationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsCreationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadArtsCreationPerformingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form =
        if (sector == "") ArtsCreationPerformingLvl3Form else ArtsCreationPerformingLvl3Form.fill(FormValues(sector))
      Ok(ArtsCreationPerformingLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitArtsCreationPerformingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsCreationPerformingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsCreationPerformingLvl3Page(formWithErrors, "")).toFuture,
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

  def loadArtsPerformingSupportActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form =
        if (sector == "") ArtsPerformingSupportActivitiesLvl4Form
        else ArtsPerformingSupportActivitiesLvl4Form.fill(FormValues(sector))
      Ok(ArtsPerformingSupportActivitiesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitArtsPerformingSupportActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsPerformingSupportActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsPerformingSupportActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadArtsSportsRecreationLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(ArtsSportsRecreationLvl2Page(ArtsSportsRecreationLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitArtsSportsRecreationLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsSportsRecreationLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsSportsRecreationLvl2Page(formWithErrors, "")).toFuture,
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

  def loadBotanicalZoologicalReservesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form =
        if (sector == "") BotanicalZoologicalReservesLvl4Form
        else BotanicalZoologicalReservesLvl4Form.fill(FormValues(sector))
      Ok(BotanicalZoologicalReservesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitBotanicalZoologicalReservesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BotanicalZoologicalReservesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BotanicalZoologicalReservesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadLibrariesArchivesCulturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form =
        if (sector == "") LibrariesArchivesCulturalLvl3Form
        else LibrariesArchivesCulturalLvl3Form.fill(FormValues(sector))
      Ok(LibrariesArchivesCulturalLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitLibrariesArchivesCulturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LibrariesArchivesCulturalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LibrariesArchivesCulturalLvl3Page(formWithErrors, "")).toFuture,
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

  def loadLibrariesArchivesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") LibrariesArchivesLvl4Form else LibrariesArchivesLvl4Form.fill(FormValues(sector))
      Ok(LibrariesArchivesLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitLibrariesArchivesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LibrariesArchivesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LibrariesArchivesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadMuseumsCollectionsMomumentsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form =
        if (sector == "") MuseumsCollectionsMomumentsLvl4Form
        else MuseumsCollectionsMomumentsLvl4Form.fill(FormValues(sector))
      Ok(MuseumsCollectionsMomumentsLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitMuseumsCollectionsMomumentsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MuseumsCollectionsMomumentsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MuseumsCollectionsMomumentsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadSportsAmusementRecreationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      val form =
        if (sector == "") SportsAmusementRecreationLvl3Form
        else SportsAmusementRecreationLvl3Form.fill(FormValues(sector))
      Ok(SportsAmusementRecreationLvl3Page(form, journey.mode)).toFuture
    }
  }

  def submitSportsAmusementRecreationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SportsAmusementRecreationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SportsAmusementRecreationLvl3Page(formWithErrors, "")).toFuture,
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

  def loadSportsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      val form = if (sector == "") SportsLvl4Form else SportsLvl4Form.fill(FormValues(sector))
      Ok(SportsLvl4Page(form, journey.mode)).toFuture
    }
  }

  def submitSportsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SportsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SportsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
}
