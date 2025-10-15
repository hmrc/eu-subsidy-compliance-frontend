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

class HouseHealthEducationController @Inject()(
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
                                             SecondaryEducationLvl4Page: SecondaryEducationLvl4Page,

                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val HouseholdsLvl2Form : Form[FormValues] = formWithSingleMandatoryField("households2")
  private val UndifferentiatedProducingActivitiesLvl4Form : Form[FormValues] = formWithSingleMandatoryField("undifferProducing4")
  private val HumanHealthLvl2Form : Form[FormValues] = formWithSingleMandatoryField("humanHealth2")
  private val HumanHealthLvl3Form : Form[FormValues] = formWithSingleMandatoryField("humanHealth3")
  private val MedicalDentalLvl4Form : Form[FormValues] = formWithSingleMandatoryField("medical4")
  private val OtherHumanHealthLvl4Form : Form[FormValues] = formWithSingleMandatoryField("otherHealth4")
  private val OtherResidentialCareLvl4Form : Form[FormValues] = formWithSingleMandatoryField("otherResidential4")
  private val OtherSocialWorkLvl4Form : Form[FormValues] = formWithSingleMandatoryField("otherSocial4")
  private val ResidentialCareLvl3Form : Form[FormValues] = formWithSingleMandatoryField("resiCare3")
  private val SocialWorkLvl3Form : Form[FormValues] = formWithSingleMandatoryField("socialWork3")
  private val EducationalSupportLvl4Form : Form[FormValues] = formWithSingleMandatoryField("educationSupport4")
  private val EducationLvl3Form : Form[FormValues] = formWithSingleMandatoryField("education3")
  private val OtherEducationLvl4Form : Form[FormValues] = formWithSingleMandatoryField("otherEducation4")
  private val SecondaryEducationLvl4Form : Form[FormValues] = formWithSingleMandatoryField("secondaryEducation4")

  def loadHouseholdsLvl2Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(HouseholdsLvl2Page(HouseholdsLvl2Form, mode)).toFuture
  }

  def submitHouseholdsLvl2Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HouseholdsLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HouseholdsLvl2Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadUndifferentiatedProducingActivitiesLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(UndifferentiatedProducingActivitiesLvl4Page(UndifferentiatedProducingActivitiesLvl4Form, mode)).toFuture
  }

  def submitUndifferentiatedProducingActivitiesLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    UndifferentiatedProducingActivitiesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(UndifferentiatedProducingActivitiesLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadHumanHealthLvl2Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(HumanHealthLvl2Page(HumanHealthLvl2Form, mode)).toFuture
  }

  def submitHumanHealthLvl2Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HumanHealthLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HumanHealthLvl2Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadHumanHealthLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(HumanHealthLvl3Page(HumanHealthLvl3Form, mode)).toFuture
  }

  def submitHumanHealthLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HumanHealthLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HumanHealthLvl3Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadMedicalDentalLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MedicalDentalLvl4Page(MedicalDentalLvl4Form, mode)).toFuture
  }

  def submitMedicalDentalLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MedicalDentalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MedicalDentalLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadOtherHumanHealthLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherHumanHealthLvl4Page(OtherHumanHealthLvl4Form, mode)).toFuture
  }

  def submitOtherHumanHealthLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherHumanHealthLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherHumanHealthLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadOtherResidentialCareLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherResidentialCareLvl4Page(OtherResidentialCareLvl4Form, mode)).toFuture
  }

  def submitOtherResidentialCareLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherResidentialCareLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherResidentialCareLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadOtherSocialWorkLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherSocialWorkLvl4Page(OtherSocialWorkLvl4Form, mode)).toFuture
  }

  def submitOtherSocialWorkLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherSocialWorkLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherSocialWorkLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadResidentialCareLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ResidentialCareLvl3Page(ResidentialCareLvl3Form, mode)).toFuture
  }

  def submitResidentialCareLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ResidentialCareLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ResidentialCareLvl3Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }

  def loadSecondaryEducationLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SecondaryEducationLvl4Page(SecondaryEducationLvl4Form, mode)).toFuture
  }

  def submitSecondaryEducationLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SecondaryEducationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SecondaryEducationLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadOtherEducationLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherEducationLvl4Page(OtherEducationLvl4Form, mode)).toFuture
  }

  def submitOtherEducationLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherEducationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherEducationLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadEducationLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(EducationLvl3Page(EducationLvl3Form, mode)).toFuture
  }

  def submitEducationLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EducationLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EducationLvl3Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadEducationalSupportLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(EducationalSupportLvl4Page(EducationalSupportLvl4Form, mode)).toFuture
  }

  def submitEducationalSupportLvl4Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    EducationalSupportLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(EducationalSupportLvl4Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
  def loadSocialWorkLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SocialWorkLvl3Page(SocialWorkLvl3Form, mode)).toFuture
  }

  def submitSocialWorkLvl3Page(mode: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SocialWorkLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SocialWorkLvl3Page(formWithErrors, mode)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, mode)).toFuture
        }
      )
  }
}