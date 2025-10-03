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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.professional._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.publicAdmin._

import javax.inject.Inject

class ProfAndPAdminController @Inject()(
                                         mcc: MessagesControllerComponents,
                                         actionBuilders: ActionBuilders,
                                         val store: Store,
                                         navigator: Navigator,
                                         PublicAdminDefenceLvl3Page: publicAdminDefenceLvl3Page,
                                         PublicAdminLvl4Page: publicAdminLvl4Page,
                                         ServiceProvisionLvl4Page: serviceProvisionLvl4Page,
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
                                         val appConfig: AppConfig
                                       ) extends BaseController(mcc) {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi

  private val   PublicAdminDefenceLvl3Form: Form[FormValues] = formWithSingleMandatoryField("admin2")
  private val   PublicAdminLvl4Form: Form[FormValues] = formWithSingleMandatoryField("building3")
  private val   ServiceProvisionLvl4Form: Form[FormValues] = formWithSingleMandatoryField("cleaning3")
  private val   AdvertisingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("employment3")
  private val   AdvertisingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("intermediation4")
  private val   ArchitecturalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("investigation4")
  private val   ArchitecturalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("equipment4")
  private val   HeadOfficesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("vehicles4")
  private val   LegalAndAccountingLvl3Form: Form[FormValues] = formWithSingleMandatoryField("office3")
  private val   OtherProfessionalLvl3Form: Form[FormValues] = formWithSingleMandatoryField("otherBusSupport4")
  private val   OtherProfessionalLvl4Form: Form[FormValues] = formWithSingleMandatoryField("personalHouse4")
  private val   ProfessionalLvl2Form: Form[FormValues] = formWithSingleMandatoryField("rental3")
  private val   ScientificRAndDLvl3Form: Form[FormValues] = formWithSingleMandatoryField("travelAgency4")
  private val   SpecialisedDesignLvl4Form: Form[FormValues] = formWithSingleMandatoryField("travel3")

  def loadPublicAdminDefenceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PublicAdminDefenceLvl3Page(PublicAdminDefenceLvl3Form)).toFuture
  }

  def submitPublicAdminDefenceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublicAdminDefenceLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublicAdminDefenceLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }


  def loadPublicAdminLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PublicAdminLvl4Page(PublicAdminLvl4Form)).toFuture
  }

  def submitPublicAdminLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PublicAdminLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PublicAdminLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadServiceProvisionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ServiceProvisionLvl4Page(ServiceProvisionLvl4Form)).toFuture
  }

  def submitServiceProvisionLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ServiceProvisionLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ServiceProvisionLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadAdvertisingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AdvertisingLvl3Page(AdvertisingLvl3Form)).toFuture
  }

  def submitAdvertisingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AdvertisingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AdvertisingLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadAdvertisingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AdvertisingLvl4Page(AdvertisingLvl4Form)).toFuture
  }

  def submitAdvertisingLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AdvertisingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AdvertisingLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadArchitecturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArchitecturalLvl3Page(ArchitecturalLvl3Form)).toFuture
  }

  def submitArchitecturalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArchitecturalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArchitecturalLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadArchitecturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ArchitecturalLvl4Page(ArchitecturalLvl4Form)).toFuture
  }

  def submitArchitecturalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ArchitecturalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ArchitecturalLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadHeadOfficesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(HeadOfficesLvl3Page(HeadOfficesLvl3Form)).toFuture
  }

  def submitHeadOfficesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HeadOfficesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HeadOfficesLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadLegalAndAccountingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(LegalAndAccountingLvl3Page(LegalAndAccountingLvl3Form)).toFuture
  }

  def submitLegalAndAccountingLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    LegalAndAccountingLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(LegalAndAccountingLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadOtherProfessionalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherProfessionalLvl3Page(OtherProfessionalLvl3Form)).toFuture
  }

  def submitOtherProfessionalLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProfessionalLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherProfessionalLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadOtherProfessionalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherProfessionalLvl4Page(OtherProfessionalLvl4Form)).toFuture
  }

  def submitOtherProfessionalLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherProfessionalLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherProfessionalLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadProfessionalLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ProfessionalLvl2Page(ProfessionalLvl2Form)).toFuture
  }

  def submitProfessionalLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ProfessionalLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ProfessionalLvl2Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadScientificRAndDLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(ScientificRAndDLvl3Page(ScientificRAndDLvl3Form)).toFuture
  }

  def submitScientificRAndDLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    ScientificRAndDLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(ScientificRAndDLvl3Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  def loadSpecialisedDesignLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(SpecialisedDesignLvl4Page(SpecialisedDesignLvl4Form)).toFuture
  }

  def submitSpecialisedDesignLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    SpecialisedDesignLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(SpecialisedDesignLvl4Page(formWithErrors)).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

}