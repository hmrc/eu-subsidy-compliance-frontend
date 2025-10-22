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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.education._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.households._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.humanHealth._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HouseHealthEducationController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  HouseholdsLvl2Page: HouseholdsLvl2Page,
  UndifferentiatedProducingActivitiesLvl4Page: UndifferentiatedProducingActivitiesLvl4Page,
  HumanHealthLvl2Page: HumanHealthLvl2Page,
  HumanHealthLvl3Page: HumanHealthLvl3Page,
  MedicalDentalLvl4Page: MedicalDentalLvl4Page,
  OtherHumanHealthLvl4Page: OtherHumanHealthLvl4Page,
  OtherResidentialCareLvl4Page: OtherResidentialCareLvl4Page,
  OtherSocialWorkLvl4Page: OtherSocialWorkLvl4Page,
  ResidentialCareLvl3Page: ResidentialCareLvl3Page,
  SocialWorkLvl3Page: SocialWorkLvl3Page,
  EducationalSupportLvl4Page: EducationalSupportLvl4Page,
  EducationLvl3Page: EducationLvl3Page,
  OtherEducationLvl4Page: OtherEducationLvl4Page,
  SecondaryEducationLvl4Page: SecondaryEducationLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val HouseholdsLvl2Form: Form[FormValues] = formWithSingleMandatoryField("households2")
  private val UndifferentiatedProducingActivitiesLvl4Form: Form[FormValues] = formWithSingleMandatoryField(
    "undifferProducing4"
  )
  private val HumanHealthLvl2Form: Form[FormValues] = formWithSingleMandatoryField("humanHealth2")
  private val HumanHealthLvl3Form: Form[FormValues] = formWithSingleMandatoryField("humanHealth3")
  private val MedicalDentalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("medical4")
  private val OtherHumanHealthLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherHealth4")
  private val OtherResidentialCareLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherResidential4")
  private val OtherSocialWorkLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherSocial4")
  private val ResidentialCareLvl3Form: Form[FormValues] = formWithSingleMandatoryField("resiCare3")
  private val SocialWorkLvl3Form: Form[FormValues] = formWithSingleMandatoryField("socialWork3")
  private val EducationalSupportLvl4Form: Form[FormValues] = formWithSingleMandatoryField("educationSupport4")
  private val EducationLvl3Form: Form[FormValues] = formWithSingleMandatoryField("education3")
  private val OtherEducationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherEducation4")
  private val SecondaryEducationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("secondaryEducation4")

  def loadHouseholdsLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(HouseholdsLvl2Page(HouseholdsLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHouseholdsLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HouseholdsLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HouseholdsLvl2Page(formWithErrors, "")).toFuture,
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
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }
  def loadUndifferentiatedProducingActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(
        UndifferentiatedProducingActivitiesLvl4Page(
          UndifferentiatedProducingActivitiesLvl4Form.fill(FormValues(sector)),
          journey.mode
        )
      ).toFuture
    }
  }

  def submitUndifferentiatedProducingActivitiesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    UndifferentiatedProducingActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(UndifferentiatedProducingActivitiesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadHumanHealthLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 2) value.toString.take(2) else value.toString
        case None => ""
      }
      Ok(HumanHealthLvl2Page(HumanHealthLvl2Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHumanHealthLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HumanHealthLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HumanHealthLvl2Page(formWithErrors, "")).toFuture,
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
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(isNaceCYA = false))
              Redirect(navigator.nextPage(form.value, journey.mode)).toFuture
            }
          }
        }
      )
  }

  def loadHumanHealthLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(HumanHealthLvl3Page(HumanHealthLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitHumanHealthLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HumanHealthLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HumanHealthLvl3Page(formWithErrors, "")).toFuture,
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

  def loadMedicalDentalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(MedicalDentalLvl4Page(MedicalDentalLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitMedicalDentalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MedicalDentalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MedicalDentalLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadOtherHumanHealthLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherHumanHealthLvl4Page(OtherHumanHealthLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherHumanHealthLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherHumanHealthLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherHumanHealthLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadOtherResidentialCareLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherResidentialCareLvl4Page(OtherResidentialCareLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherResidentialCareLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherResidentialCareLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherResidentialCareLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOtherSocialWorkLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherSocialWorkLvl4Page(OtherSocialWorkLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherSocialWorkLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherSocialWorkLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherSocialWorkLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadResidentialCareLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(ResidentialCareLvl3Page(ResidentialCareLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitResidentialCareLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ResidentialCareLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ResidentialCareLvl3Page(formWithErrors, "")).toFuture,
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

  def loadSecondaryEducationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(SecondaryEducationLvl4Page(SecondaryEducationLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitSecondaryEducationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SecondaryEducationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SecondaryEducationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadOtherEducationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(OtherEducationLvl4Page(OtherEducationLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitOtherEducationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherEducationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherEducationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadEducationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(EducationLvl3Page(EducationLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitEducationLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EducationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EducationLvl3Page(formWithErrors, "")).toFuture,
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

  def loadEducationalSupportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => value.toString
        case None => ""
      }
      Ok(EducationalSupportLvl4Page(EducationalSupportLvl4Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitEducationalSupportLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EducationalSupportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EducationalSupportLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadSocialWorkLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
      val sector = journey.sector.value match {
        case Some(value) => if (value.toString.length > 4) value.toString.take(4) else value.toString
        case None => ""
      }
      Ok(SocialWorkLvl3Page(SocialWorkLvl3Form.fill(FormValues(sector)), journey.mode)).toFuture
    }
  }

  def submitSocialWorkLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SocialWorkLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SocialWorkLvl3Page(formWithErrors, "")).toFuture,
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
}
