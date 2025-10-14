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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.manufacturing.paperPrinted._

import javax.inject.Inject

class PaperPrintedController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             ArticlesPaperPaperboardLvl4Page: ArticlesPaperPaperboardLvl4Page,
                                             PaperLvl3Page: PaperLvl3Page,
                                             PrintedLvl3Page: PrintedLvl3Page,
                                             PrintingServicesLvl4Page: PrintingServicesLvl4Page,
                                             PulpPaperPaperboardLvl4Page: PulpPaperPaperboardLvl4Page,

                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._

  private val ArticlesPaperPaperboardLvl4Form: Form[FormValues] = formWithSingleMandatoryField("paper4")
  private val PaperLvl3Form: Form[FormValues] = formWithSingleMandatoryField("paper3")
  private val PrintedLvl3Form: Form[FormValues] = formWithSingleMandatoryField("printed3")
  private val PrintingServicesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("printing4")
  private val PulpPaperPaperboardLvl4Form: Form[FormValues] = formWithSingleMandatoryField("pulp4")



  def loadArticlesPaperPaperboardLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArticlesPaperPaperboardLvl4Page(ArticlesPaperPaperboardLvl4Form, isUpdate)).toFuture
  }

  def submitArticlesPaperPaperboardLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArticlesPaperPaperboardLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArticlesPaperPaperboardLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }


  def loadPaperLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PaperLvl3Page(PaperLvl3Form, isUpdate)).toFuture
  }

  def submitPaperLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PaperLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PaperLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadPrintedLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PrintedLvl3Page(PrintedLvl3Form, isUpdate)).toFuture
  }

  def submitPrintedLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PrintedLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PrintedLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }



  def loadPrintingServicesLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PrintingServicesLvl4Page(PrintingServicesLvl4Form, isUpdate)).toFuture
  }

  def submitPrintingServicesLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PrintingServicesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PrintingServicesLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadPulpPaperPaperboardLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PulpPaperPaperboardLvl4Page(PulpPaperPaperboardLvl4Form, isUpdate)).toFuture
  }

  def submitPulpPaperPaperboardLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PulpPaperPaperboardLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PulpPaperPaperboardLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }
}