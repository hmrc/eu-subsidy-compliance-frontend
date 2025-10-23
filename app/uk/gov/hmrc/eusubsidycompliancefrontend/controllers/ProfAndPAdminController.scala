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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.professional._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.publicAdmin._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ProfAndPAdminController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  PublicAdminDefenceLvl3Page: PublicAdminDefenceLvl3Page,
  PublicAdminLvl4Page: PublicAdminLvl4Page,
  ServiceProvisionLvl4Page: ServiceProvisionLvl4Page,
  AdvertisingLvl3Page: AdvertisingLvl3Page,
  AdvertisingLvl4Page: AdvertisingLvl4Page,
  ArchitecturalLvl3Page: ArchitecturalLvl3Page,
  ArchitecturalLvl4Page: ArchitecturalLvl4Page,
  HeadOfficesLvl3Page: HeadOfficesLvl3Page,
  LegalAndAccountingLvl3Page: LegalAndAccountingLvl3Page,
  OtherProfessionalLvl3Page: OtherProfessionalLvl3Page,
  OtherProfessionalLvl4Page: OtherProfessionalLvl4Page,
  ProfessionalLvl2Page: ProfessionalLvl2Page,
  ScientificRAndDLvl3Page: ScientificRAndDLvl3Page,
  SpecialisedDesignLvl4Page: SpecialisedDesignLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi

  private val PublicAdminDefenceLvl3Form: Form[FormValues] = formWithSingleMandatoryField("publicAdmin3")
  private val PublicAdminLvl4Form: Form[FormValues] = formWithSingleMandatoryField("publicAdmin4")
  private val ServiceProvisionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("serviceProvision4")
  private val AdvertisingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("advertising3")
  private val AdvertisingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("advertising4")
  private val ArchitecturalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("architecture3")
  private val ArchitecturalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("architecture4")
  private val HeadOfficesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("headOffice3")
  private val LegalAndAccountingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("legal3")
  private val OtherProfessionalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherProf3")
  private val OtherProfessionalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherProf4")
  private val ProfessionalLvl2Form: Form[FormValues] = formWithSingleMandatoryField("prof2")
  private val ScientificRAndDLvl3Form: Form[FormValues] = formWithSingleMandatoryField("rAndD3")
  private val SpecialisedDesignLvl4Form: Form[FormValues] = formWithSingleMandatoryField("specialDesign4")

  def loadPublicAdminDefenceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(PublicAdminDefenceLvl3Page(PublicAdminDefenceLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPublicAdminDefenceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublicAdminDefenceLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublicAdminDefenceLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadPublicAdminLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(PublicAdminLvl4Page(PublicAdminLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitPublicAdminLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublicAdminLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublicAdminLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadServiceProvisionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(ServiceProvisionLvl4Page(ServiceProvisionLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitServiceProvisionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ServiceProvisionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ServiceProvisionLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAdvertisingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(AdvertisingLvl3Page(AdvertisingLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAdvertisingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AdvertisingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AdvertisingLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadAdvertisingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(AdvertisingLvl4Page(AdvertisingLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitAdvertisingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AdvertisingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AdvertisingLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadArchitecturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(ArchitecturalLvl3Page(ArchitecturalLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitArchitecturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArchitecturalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArchitecturalLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadArchitecturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(ArchitecturalLvl4Page(ArchitecturalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitArchitecturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArchitecturalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArchitecturalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadHeadOfficesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(HeadOfficesLvl3Page(HeadOfficesLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHeadOfficesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HeadOfficesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HeadOfficesLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadLegalAndAccountingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(LegalAndAccountingLvl3Page(LegalAndAccountingLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitLegalAndAccountingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LegalAndAccountingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LegalAndAccountingLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadOtherProfessionalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(OtherProfessionalLvl3Page(OtherProfessionalLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherProfessionalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProfessionalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherProfessionalLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadOtherProfessionalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherProfessionalLvl4Page(OtherProfessionalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherProfessionalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProfessionalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherProfessionalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadProfessionalLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(ProfessionalLvl2Page(ProfessionalLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitProfessionalLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ProfessionalLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ProfessionalLvl2Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadScientificRAndDLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(ScientificRAndDLvl3Page(ScientificRAndDLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitScientificRAndDLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ScientificRAndDLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ScientificRAndDLvl3Page(formWithErrors, "")).toFuture,
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
                updatedSector <- store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
                updatedStoreFlags <- store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              }
              yield Redirect(navigator.nextPage(form.value, journey.mode))
            }
          }
        }
      )
  }

  def loadSpecialisedDesignLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(SpecialisedDesignLvl4Page(SpecialisedDesignLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitSpecialisedDesignLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SpecialisedDesignLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SpecialisedDesignLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
}
