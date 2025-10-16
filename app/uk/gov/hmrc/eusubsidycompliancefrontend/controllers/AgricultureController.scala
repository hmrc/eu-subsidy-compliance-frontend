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
import play.twirl.api.Html
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.agriculture._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.forestry._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.fishing._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class AgricultureController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             AgricultureLvl3Page: AgricultureLvl3Page,
                                             AnimalProductionLvl4Page: AnimalProductionLvl4Page,
                                             NonPerennialCropLvl4Page: NonPerennialCropLvl4Page,
                                             perennialCropLvl4Page: PerennialCropLvl4Page,
                                             SupportActivitiesLvl4Page: SupportActivitiesLvl4Page,
                                             ForestryLvl3Page: ForestryLvl3Page,
                                             FishingAndAquacultureLvl3Page: FishingAndAquacultureLvl3Page,
                                             AquacultureLvl4Page: AquacultureLvl4Page,
                                             FishingLvl4Page: FishingLvl4Page,
                                           )(implicit
                                             val appConfig: AppConfig
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

  def loadAgricultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AgricultureLvl3Page(AgricultureLvl3Form, "")).toFuture
  }

  def submitAgricultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AgricultureLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AgricultureLvl3Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadSupportActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SupportActivitiesLvl4Page(SupportActivitiesLvl4Form, "")).toFuture
  }

  def submitSupportActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SupportActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SupportActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadAnimalProductionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AnimalProductionLvl4Page(AnimalProductionLvl4Form, "")).toFuture
  }

  def submitAnimalProductionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AnimalProductionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AnimalProductionLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadPerennialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(perennialCropLvl4Page(PerennialCropLvl4Form, "")).toFuture
  }

  def submitPerennialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PerennialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(perennialCropLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadNonPerennialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(NonPerennialCropLvl4Page(NonPerennialCropLvl4Form, "")).toFuture
  }

  def submitNonPerennialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    NonPerennialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NonPerennialCropLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadForestryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ForestryLvl3Page(ForestryLvl3Form, "")).toFuture
  }

  def submitForestryLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ForestryLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ForestryLvl3Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadFishingAndAquacultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FishingAndAquacultureLvl3Page(FishingAndAquacultureLvl3Form, "")).toFuture
  }

  def submitFishingAndAquacultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FishingAndAquacultureLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FishingAndAquacultureLvl3Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAquacultureLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AquacultureLvl4Page(AquacultureLvl4Form, "")).toFuture
  }

  def submitAquacultureLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AquacultureLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AquacultureLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadFishingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FishingLvl4Page(FishingLvl4Form, "")).toFuture
  }

  def submitFishingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FishingLvl4Page(formWithErrors, "")).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }


}