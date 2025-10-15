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

class ArtsController @Inject()(
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
                                SportsLvl4Page: SportsLvl4Page,


                              )(implicit
                                val appConfig: AppConfig
                              ) extends BaseController(mcc) {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi

  private val AmusementAndRecreationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("amusement4")
  private val ArtsCreationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("artsCreation4")
  private val ArtsCreationPerformingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("artsCreation3")
  private val ArtsPerformingSupportActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("artsPerforming4")
  private val ArtsSportsRecreationLvl2Form: Form[FormValues] = formWithSingleMandatoryField("artsSports2")
  private val BotanicalZoologicalReservesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("zoo4")
  private val LibrariesArchivesCulturalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("libraries3")
  private val LibrariesArchivesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("libraries4")
  private val MuseumsCollectionsMomumentsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("museums4")
  private val SportsAmusementRecreationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("sports3")
  private val SportsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("sports4")


  def loadAmusementAndRecreationLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AmusementAndRecreationLvl4Page(AmusementAndRecreationLvl4Form, mode)).toFuture
  }

  def submitAmusementAndRecreationLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AmusementAndRecreationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AmusementAndRecreationLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }


  def loadArtsCreationLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArtsCreationLvl4Page(ArtsCreationLvl4Form, mode)).toFuture
  }

  def submitArtsCreationLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsCreationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsCreationLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadArtsCreationPerformingLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArtsCreationPerformingLvl3Page(ArtsCreationPerformingLvl3Form, mode)).toFuture
  }

  def submitArtsCreationPerformingLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsCreationPerformingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsCreationPerformingLvl3Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadArtsPerformingSupportActivitiesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArtsPerformingSupportActivitiesLvl4Page(ArtsPerformingSupportActivitiesLvl4Form, mode)).toFuture
  }

  def submitArtsPerformingSupportActivitiesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsPerformingSupportActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsPerformingSupportActivitiesLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadArtsSportsRecreationLvl2Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArtsSportsRecreationLvl2Page(ArtsSportsRecreationLvl2Form, mode)).toFuture
  }

  def submitArtsSportsRecreationLvl2Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArtsSportsRecreationLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArtsSportsRecreationLvl2Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadBotanicalZoologicalReservesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(BotanicalZoologicalReservesLvl4Page(BotanicalZoologicalReservesLvl4Form, mode)).toFuture
  }

  def submitBotanicalZoologicalReservesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BotanicalZoologicalReservesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BotanicalZoologicalReservesLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadLibrariesArchivesCulturalLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LibrariesArchivesCulturalLvl3Page(LibrariesArchivesCulturalLvl3Form, mode)).toFuture
  }

  def submitLibrariesArchivesCulturalLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LibrariesArchivesCulturalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LibrariesArchivesCulturalLvl3Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadLibrariesArchivesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LibrariesArchivesLvl4Page(LibrariesArchivesLvl4Form, mode)).toFuture
  }

  def submitLibrariesArchivesLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LibrariesArchivesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LibrariesArchivesLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadMuseumsCollectionsMomumentsLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MuseumsCollectionsMomumentsLvl4Page(MuseumsCollectionsMomumentsLvl4Form, mode)).toFuture
  }

  def submitMuseumsCollectionsMomumentsLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MuseumsCollectionsMomumentsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MuseumsCollectionsMomumentsLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadSportsAmusementRecreationLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SportsAmusementRecreationLvl3Page(SportsAmusementRecreationLvl3Form, mode)).toFuture
  }

  def submitSportsAmusementRecreationLvl3Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SportsAmusementRecreationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SportsAmusementRecreationLvl3Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadSportsLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SportsLvl4Page(SportsLvl4Form, mode)).toFuture
  }

  def submitSportsLvl4Page(mode: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SportsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SportsLvl4Page(formWithErrors, mode)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
}