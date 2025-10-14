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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.mining._

import javax.inject.Inject

class MiningController @Inject()(
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             val store: Store,
                                             navigator: Navigator,
                                             CoalMiningLvl3Page: CoalMiningLvl3Page,
                                             GasMiningLvl3Page: GasMiningLvl3Page,
                                             MetalMiningLvl3Page: MetalMiningLvl3Page,
                                             MiningLvl2Page: MiningLvl2Page,
                                             MiningSupportLvl3Page: MiningSupportLvl3Page,
                                             NonFeMetalMiningLvl4Page: NonFeMetalMiningLvl4Page,
                                             OtherMiningLvl3Page: OtherMiningLvl3Page,
                                             OtherMiningLvl4Page: OtherMiningLvl4Page,
                                             QuarryingLvl4Page: QuarryingLvl4Page,
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val CoalMiningLvl3Form : Form[FormValues] = formWithSingleMandatoryField("coal3")
  private val GasMiningLvl3Form : Form[FormValues] = formWithSingleMandatoryField("gas3")
  private val MetalMiningLvl3Form : Form[FormValues] = formWithSingleMandatoryField("metal3")
  private val MiningLvl2Form : Form[FormValues] = formWithSingleMandatoryField("mining2")
  private val MiningSupportLvl3Form : Form[FormValues] = formWithSingleMandatoryField("miningSupport3")
  private val NonFeMetalMiningLvl4Form : Form[FormValues] = formWithSingleMandatoryField("nonIron4")
  private val OtherMiningLvl3Form : Form[FormValues] = formWithSingleMandatoryField("otherMining3")
  private val OtherMiningLvl4Form : Form[FormValues] = formWithSingleMandatoryField("otherMining4")
  private val QuarryingLvl4Form: Form[FormValues] = formWithSingleMandatoryField("quarrying4")

  def loadMiningLvl2Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MiningLvl2Page(MiningLvl2Form, isUpdate)).toFuture
  }

  def submitMiningLvl2Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MiningLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MiningLvl2Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }
  def loadMiningSupportLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MiningSupportLvl3Page(MiningSupportLvl3Form, isUpdate)).toFuture
  }

  def submitMiningSupportLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MiningSupportLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MiningSupportLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }
  def loadNonFeMetalMiningLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(NonFeMetalMiningLvl4Page(NonFeMetalMiningLvl4Form, isUpdate)).toFuture
  }

  def submitNonFeMetalMiningLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    NonFeMetalMiningLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(NonFeMetalMiningLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadOtherMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherMiningLvl3Page(OtherMiningLvl3Form, isUpdate)).toFuture
  }

  def submitOtherMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherMiningLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherMiningLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadOtherMiningLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherMiningLvl4Page(OtherMiningLvl4Form, isUpdate)).toFuture
  }

  def submitOtherMiningLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherMiningLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherMiningLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }
  def loadQuarryingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(QuarryingLvl4Page(QuarryingLvl4Form, isUpdate)).toFuture
  }

  def submitQuarryingLvl4Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    QuarryingLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(QuarryingLvl4Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }
  def loadCoalMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(CoalMiningLvl3Page(CoalMiningLvl3Form, isUpdate)).toFuture
  }

  def submitCoalMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    CoalMiningLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(CoalMiningLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadGasMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(GasMiningLvl3Page(GasMiningLvl3Form, isUpdate)).toFuture
  }

  def submitGasMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    GasMiningLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(GasMiningLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

  def loadMetalMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MetalMiningLvl3Page(MetalMiningLvl3Form, isUpdate)).toFuture
  }

  def submitMetalMiningLvl3Page(isUpdate: Boolean) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MetalMiningLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MetalMiningLvl3Page(formWithErrors, isUpdate)).toFuture,
        form =>{
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, isUpdate)).toFuture
        }
      )
  }

}