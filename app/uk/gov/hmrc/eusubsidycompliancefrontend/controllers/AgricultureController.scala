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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.agriculture._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class AgricultureController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             AgricultureLvl3Page: AgricultureLvl3Page,
                                             AnimalProductionLvl4Page: AnimalProductionLvl4Page,
                                             NonPerennnialCropLvl4Page: NonPerennnialCropLvl4Page,
                                             PerennnialCropLvl4Page: PerennnialCropLvl4Page,
                                             SupportActivitiesLvl4Page: SupportActivitiesLvl4Page
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val SupportActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("agriSupport4")
  private val PerennnialCropLvl4Form: Form[FormValues] = formWithSingleMandatoryField("pCrops4")
  private val NonPerennnialCropLvl4Form: Form[FormValues] = formWithSingleMandatoryField("nonPCrops4")
  private val AnimalProductionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("animal4")
  private val AgricultureLvl3Form: Form[FormValues] = formWithSingleMandatoryField("agriculture3")

  def loadAgricultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AgricultureLvl3Page(AgricultureLvl3Form)).toFuture
  }

  def submitAgricultureLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AgricultureLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AgricultureLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadSupportActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SupportActivitiesLvl4Page(SupportActivitiesLvl4Form)).toFuture
  }

  def submitSupportActivitiesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SupportActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SupportActivitiesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
  def loadAnimalProductionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AnimalProductionLvl4Page(AnimalProductionLvl4Form)).toFuture
  }

  def submitAnimalProductionLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AnimalProductionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AnimalProductionLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadPerennnialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PerennnialCropLvl4Page(PerennnialCropLvl4Form)).toFuture
  }

  def submitPerennnialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PerennnialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PerennnialCropLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadNonPerennnialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(NonPerennnialCropLvl4Page(NonPerennnialCropLvl4Form)).toFuture
  }

  def submitNonPerennnialCropLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    NonPerennnialCropLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NonPerennnialCropLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

}