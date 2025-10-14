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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.publishing._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.telecoms._

import javax.inject.Inject

class PublishingTelecomsController @Inject()(
                                              mcc: MessagesControllerComponents,
                                              actionBuilders: ActionBuilders,
                                              val store: Store,
                                              navigator: Navigator,
                                              ComputerInfrastructureDataHostingLvl3Page: ComputerInfrastructureDataHostingLvl3Page,
                                              ComputerProgrammingConsultancyLvl3Page: ComputerProgrammingConsultancyLvl3Page,
                                              TelecommunicationLvl2Page: TelecommunicationLvl2Page,
                                              TelecommunicationLvl3Page: TelecommunicationLvl3Page,
                                              WebSearchPortalLvl4Page: WebSearchPortalLvl4Page,
                                              BookPublishingLvl4Page: BookPublishingLvl4Page,
                                              FilmMusicPublishingLvl3Page: FilmMusicPublishingLvl3Page,
                                              FilmVideoActivitiesLvl4Page: FilmVideoActivitiesLvl4Page,
                                              NewsOtherContentDistributionLvl4Page: NewsOtherContentDistributionLvl4Page,
                                              ProgrammingBroadcastingDistributionLvl3Page: ProgrammingBroadcastingDistributionLvl3Page,
                                              PublishingLvl2Page: PublishingLvl2Page,
                                              PublishingLvl3Page: PublishingLvl3Page,
                                              SoftwarePublishingLvl4Page: SoftwarePublishingLvl4Page,


                                            )(implicit
                                              val appConfig: AppConfig
                                            ) extends BaseController(mcc) {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi

  private val ComputerInfrastructureDataHostingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("infra3")
  private val ComputerProgrammingConsultancyLvl3Form: Form[FormValues] = formWithSingleMandatoryField("programming3")
  private val TelecommunicationLvl2Form: Form[FormValues] = formWithSingleMandatoryField("telecommunication2")
  private val TelecommunicationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("telecommunication3")
  private val WebSearchPortalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("webSearch4")
  private val BookPublishingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("book4")
  private val FilmMusicPublishingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("filmPublishing3")
  private val FilmVideoActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("film4")
  private val NewsOtherContentDistributionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("news4")
  private val ProgrammingBroadcastingDistributionLvl3Form: Form[FormValues] = formWithSingleMandatoryField("broadcasting3")
  private val PublishingLvl2Form: Form[FormValues] = formWithSingleMandatoryField("publishing2")
  private val PublishingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("publishing3")
  private val SoftwarePublishingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("softwarePublishing4")

  def loadComputerInfrastructureDataHostingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ComputerInfrastructureDataHostingLvl3Page(ComputerInfrastructureDataHostingLvl3Form, isUpdate)).toFuture
  }

  def submitComputerInfrastructureDataHostingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputerInfrastructureDataHostingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComputerInfrastructureDataHostingLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadComputerProgrammingConsultancyLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ComputerProgrammingConsultancyLvl3Page(ComputerProgrammingConsultancyLvl3Form, isUpdate)).toFuture
  }

  def submitComputerProgrammingConsultancyLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputerProgrammingConsultancyLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComputerProgrammingConsultancyLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadTelecommunicationLvl2Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(TelecommunicationLvl2Page(TelecommunicationLvl2Form, isUpdate)).toFuture
  }

  def submitTelecommunicationLvl2Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TelecommunicationLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TelecommunicationLvl2Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadTelecommunicationLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(TelecommunicationLvl3Page(TelecommunicationLvl3Form, isUpdate)).toFuture
  }

  def submitTelecommunicationLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TelecommunicationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TelecommunicationLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadWebSearchPortalLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(WebSearchPortalLvl4Page(WebSearchPortalLvl4Form, isUpdate)).toFuture
  }

  def submitWebSearchPortalLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WebSearchPortalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WebSearchPortalLvl4Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadBookPublishingLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(BookPublishingLvl4Page(BookPublishingLvl4Form, isUpdate)).toFuture
  }

  def submitBookPublishingLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BookPublishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BookPublishingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadFilmMusicPublishingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FilmMusicPublishingLvl3Page(FilmMusicPublishingLvl3Form, isUpdate)).toFuture
  }

  def submitFilmMusicPublishingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FilmMusicPublishingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FilmMusicPublishingLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadFilmVideoActivitiesLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FilmVideoActivitiesLvl4Page(FilmVideoActivitiesLvl4Form, isUpdate)).toFuture
  }

  def submitFilmVideoActivitiesLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FilmVideoActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FilmVideoActivitiesLvl4Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadNewsOtherContentDistributionLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(NewsOtherContentDistributionLvl4Page(NewsOtherContentDistributionLvl4Form, isUpdate)).toFuture
  }

  def submitNewsOtherContentDistributionLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    NewsOtherContentDistributionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NewsOtherContentDistributionLvl4Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadProgrammingBroadcastingDistributionLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ProgrammingBroadcastingDistributionLvl3Page(ProgrammingBroadcastingDistributionLvl3Form, isUpdate)).toFuture
  }

  def submitProgrammingBroadcastingDistributionLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ProgrammingBroadcastingDistributionLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ProgrammingBroadcastingDistributionLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadPublishingLvl2Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PublishingLvl2Page(PublishingLvl2Form, isUpdate)).toFuture
  }

  def submitPublishingLvl2Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublishingLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublishingLvl2Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadPublishingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PublishingLvl3Page(PublishingLvl3Form, isUpdate)).toFuture
  }

  def submitPublishingLvl3Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublishingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublishingLvl3Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadSoftwarePublishingLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SoftwarePublishingLvl4Page(SoftwarePublishingLvl4Form, isUpdate)).toFuture
  }

  def submitSoftwarePublishingLvl4Page(isUpdate: Boolean): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SoftwarePublishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SoftwarePublishingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }


}