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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{Journey, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.other.{HairdressingLvl4Page, HouseholdRepairLvl4Page, MembershipOrgActivitiesLvl3Page, MembershipOrgsLvl4Page, MotorVehiclesRepairLvl4Page, OtherLvl2Page, OtherMembershipOrgsLvl4Page, OtherPersonalServicesLvl4Page, PersonalServicesLvl3Page, RepairsLvl3Page}

import javax.inject.Inject

class OtherServicesController @Inject() (
                                          mcc: MessagesControllerComponents,
                                          actionBuilders: ActionBuilders,
                                          val store: Store,
                                          navigator: Navigator,
                                          otherLvl2Page: OtherLvl2Page,
                                          membershipOrgActivitiesLvl3Page: MembershipOrgActivitiesLvl3Page,
                                          personalServicesLvl3Page: PersonalServicesLvl3Page,
                                          repairsLvl3Page: RepairsLvl3Page,
                                          hairdressingLvl4Page: HairdressingLvl4Page,
                                          householdRepairLvl4Page: HouseholdRepairLvl4Page,
                                          membershipOrgsLvl4Page: MembershipOrgsLvl4Page,
                                          motorVehiclesRepairLvl4Page: MotorVehiclesRepairLvl4Page,
                                          otherMembershipOrgsLvl4Page: OtherMembershipOrgsLvl4Page,
                                          otherPersonalServicesLvl4Page: OtherPersonalServicesLvl4Page)
          (implicit val appConfig: AppConfig) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi
  private val otherLvl2PageForm: Form[FormValues] = formWithSingleMandatoryField("other2")
  private val membershipOrgActivitiesLvl3PageForm: Form[FormValues] = formWithSingleMandatoryField("membership3")
  private val personalServicesLvl3PageForm: Form[FormValues] = formWithSingleMandatoryField("personalServices3")
  private val repairsLvl3PageForm: Form[FormValues] = formWithSingleMandatoryField("repairs3")
  private val hairdressingLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("hairdressing4")
  private val householdRepairLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("householdRepair4")
  private val membershipOrgsLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("membership4")
  private val motorVehiclesRepairLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("vehiclesRepair4")
  private val otherMembershipOrgsLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("otherMembership4")
  private val otherPersonalServicesLvl4PageForm: Form[FormValues] = formWithSingleMandatoryField("otherPersonal4")

  // otherLvl2Page
  def loadOtherLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherLvl2Page(otherLvl2PageForm,false)).toFuture
  }

  def submitOtherLvl2Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherLvl2PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherLvl2Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //membershipOrgActivitiesLvl3Page
  def loadMembershipOrgActivitiesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(membershipOrgActivitiesLvl3Page(membershipOrgActivitiesLvl3PageForm,false)).toFuture
  }

  def submitMembershipOrgActivitiesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    membershipOrgActivitiesLvl3PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(membershipOrgActivitiesLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //personalServicesLvl3Page
  def loadPersonalServicesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(personalServicesLvl3Page(personalServicesLvl3PageForm, false)).toFuture
  }

  def submitPersonalServicesLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    personalServicesLvl3PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(personalServicesLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //repairsLvl3Page
  def loadRepairsLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(repairsLvl3Page(repairsLvl3PageForm, false)).toFuture
  }

  def submitRepairsLvl3Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    repairsLvl3PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(repairsLvl3Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //hairdressingLvl4Page
  def loadHairdressingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(hairdressingLvl4Page(hairdressingLvl4PageForm, false)).toFuture
  }

  def submitHairdressingLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    hairdressingLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(hairdressingLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //householdRepairLvl4Page
  def loadHouseholdRepairLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(householdRepairLvl4Page(householdRepairLvl4PageForm, false)).toFuture
  }

  def submitHouseholdRepairLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    householdRepairLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(householdRepairLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //membershipOrgsLvl4Page
  def loadMembershipOrgsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(membershipOrgsLvl4Page(membershipOrgsLvl4PageForm, false)).toFuture
  }

  def submitMembershipOrgsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    membershipOrgsLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(membershipOrgsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //motorVehiclesRepairLvl4Page
  def loadMotorVehiclesRepairLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(motorVehiclesRepairLvl4Page(motorVehiclesRepairLvl4PageForm, false)).toFuture
  }

  def submitMotorVehiclesRepairLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    motorVehiclesRepairLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(motorVehiclesRepairLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //otherMembershipOrgsLvl4Page
  def loadOtherMembershipOrgsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherMembershipOrgsLvl4Page(otherMembershipOrgsLvl4PageForm, false)).toFuture
  }

  def submitOtherMembershipOrgsLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherMembershipOrgsLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherMembershipOrgsLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }

  //otherPersonalServicesLvl4Page
  def loadOtherPersonalServicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(otherPersonalServicesLvl4Page(otherPersonalServicesLvl4PageForm, false)).toFuture
  }

  def submitOtherPersonalServicesLvl4Page() : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    otherPersonalServicesLvl4PageForm.bindFromRequest()
      .fold(
        formWithErrors => BadRequest(otherPersonalServicesLvl4Page(formWithErrors)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate = false)).toFuture
        }
      )
  }
}
