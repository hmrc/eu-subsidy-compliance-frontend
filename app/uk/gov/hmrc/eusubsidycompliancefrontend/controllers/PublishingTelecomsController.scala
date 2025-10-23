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
import scala.concurrent.ExecutionContext

class PublishingTelecomsController @Inject() (
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
  SoftwarePublishingLvl4Page: SoftwarePublishingLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
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
  private val ProgrammingBroadcastingDistributionLvl3Form: Form[FormValues] = formWithSingleMandatoryField(
    "broadcasting3"
  )
  private val PublishingLvl2Form: Form[FormValues] = formWithSingleMandatoryField("publishing2")
  private val PublishingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("publishing3")
  private val SoftwarePublishingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("softwarePublishing4")

  def loadComputerInfrastructureDataHostingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        ComputerInfrastructureDataHostingLvl3Page(
          ComputerInfrastructureDataHostingLvl3Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitComputerInfrastructureDataHostingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputerInfrastructureDataHostingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComputerInfrastructureDataHostingLvl3Page(formWithErrors, "")).toFuture,
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

  def loadComputerProgrammingConsultancyLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        ComputerProgrammingConsultancyLvl3Page(
          ComputerProgrammingConsultancyLvl3Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitComputerProgrammingConsultancyLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ComputerProgrammingConsultancyLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ComputerProgrammingConsultancyLvl3Page(formWithErrors, "")).toFuture,
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

  def loadTelecommunicationLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(TelecommunicationLvl2Page(TelecommunicationLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitTelecommunicationLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TelecommunicationLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TelecommunicationLvl2Page(formWithErrors, "")).toFuture,
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

  def loadTelecommunicationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(TelecommunicationLvl3Page(TelecommunicationLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitTelecommunicationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TelecommunicationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TelecommunicationLvl3Page(formWithErrors, "")).toFuture,
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

  def loadWebSearchPortalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(WebSearchPortalLvl4Page(WebSearchPortalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitWebSearchPortalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    WebSearchPortalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(WebSearchPortalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadBookPublishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(BookPublishingLvl4Page(BookPublishingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitBookPublishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    BookPublishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(BookPublishingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadFilmMusicPublishingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(FilmMusicPublishingLvl3Page(FilmMusicPublishingLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFilmMusicPublishingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FilmMusicPublishingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FilmMusicPublishingLvl3Page(formWithErrors, "")).toFuture,
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

  def loadFilmVideoActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(FilmVideoActivitiesLvl4Page(FilmVideoActivitiesLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitFilmVideoActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FilmVideoActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FilmVideoActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadNewsOtherContentDistributionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        NewsOtherContentDistributionLvl4Page(
          NewsOtherContentDistributionLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitNewsOtherContentDistributionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    NewsOtherContentDistributionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NewsOtherContentDistributionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadProgrammingBroadcastingDistributionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(
        ProgrammingBroadcastingDistributionLvl3Page(
          ProgrammingBroadcastingDistributionLvl3Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitProgrammingBroadcastingDistributionLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ProgrammingBroadcastingDistributionLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ProgrammingBroadcastingDistributionLvl3Page(formWithErrors, "")).toFuture,
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

  def loadPublishingLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(PublishingLvl2Page(PublishingLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPublishingLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublishingLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublishingLvl2Page(formWithErrors, "")).toFuture,
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

  def loadPublishingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(PublishingLvl3Page(PublishingLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPublishingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublishingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublishingLvl3Page(formWithErrors, "")).toFuture,
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

  def loadSoftwarePublishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(SoftwarePublishingLvl4Page(SoftwarePublishingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitSoftwarePublishingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SoftwarePublishingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SoftwarePublishingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

}
