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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.finance._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.realestate._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FinanceRealEstateController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  navigator: Navigator,
  FeeContractLvl4Page: FeeContractLvl4Page,
  PropertyDevelopmentLvl4Page: PropertyDevelopmentLvl4Page,
  RealEstateLvl3Page: RealEstateLvl3Page,
  AuxiliaryFinancialLvl3Page: AuxiliaryFinancialLvl3Page,
  AuxiliaryInsuranceLvl4Page: AuxiliaryInsuranceLvl4Page,
  AuxiliaryNonInsuranceLvl4Page: AuxiliaryNonInsuranceLvl4Page,
  FinanceInsuranceLvl2Page: FinanceInsuranceLvl2Page,
  FinancialServicesLvl3Page: FinancialServicesLvl3Page,
  HoldingCompaniesLvl4Page: HoldingCompaniesLvl4Page,
  InsuranceLvl3Page: InsuranceLvl3Page,
  InsuranceTypeLvl4Page: InsuranceTypeLvl4Page,
  MonetaryIntermediationLvl4Page: MonetaryIntermediationLvl4Page,
  OtherFinancialLvl4Page: OtherFinancialLvl4Page,
  TrustsFundsLvl4Page: TrustsFundsLvl4Page
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val FeeContractLvl4Form: Form[FormValues] = formWithSingleMandatoryField("feeContract4")
  private val PropertyDevelopmentLvl4Form: Form[FormValues] = formWithSingleMandatoryField("propertyDev4")
  private val RealEstateLvl3Form: Form[FormValues] = formWithSingleMandatoryField("realEstate3")
  private val AuxiliaryFinancialLvl3Form: Form[FormValues] = formWithSingleMandatoryField("auxFinance3")
  private val AuxiliaryInsuranceLvl4Form: Form[FormValues] = formWithSingleMandatoryField("auxInsurance4")
  private val AuxiliaryNonInsuranceLvl4Form: Form[FormValues] = formWithSingleMandatoryField("auxNonInsurance4")
  private val FinanceInsuranceLvl2Form: Form[FormValues] = formWithSingleMandatoryField("finance2")
  private val FinancialServicesLvl3Form: Form[FormValues] = formWithSingleMandatoryField("financial3")
  private val HoldingCompaniesLvl4Form: Form[FormValues] = formWithSingleMandatoryField("holding4")
  private val InsuranceLvl3Form: Form[FormValues] = formWithSingleMandatoryField("insurance3")
  private val InsuranceTypeLvl4Form: Form[FormValues] = formWithSingleMandatoryField("insurance4")
  private val MonetaryIntermediationLvl4Form: Form[FormValues] = formWithSingleMandatoryField("monetary4")
  private val OtherFinancialLvl4Form: Form[FormValues] = formWithSingleMandatoryField("otherFinance4")
  private val TrustsFundsLvl4Form: Form[FormValues] = formWithSingleMandatoryField("trusts4")

  def loadFeeContractLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FeeContractLvl4Page(FeeContractLvl4Form, "")).toFuture
  }

  def submitFeeContractLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FeeContractLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FeeContractLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadPropertyDevelopmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(PropertyDevelopmentLvl4Page(PropertyDevelopmentLvl4Form, "")).toFuture
  }

  def submitPropertyDevelopmentLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    PropertyDevelopmentLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(PropertyDevelopmentLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadRealEstateLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(RealEstateLvl3Page(RealEstateLvl3Form, "")).toFuture
  }

  def submitRealEstateLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    RealEstateLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(RealEstateLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadAuxiliaryFinancialLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AuxiliaryFinancialLvl3Page(AuxiliaryFinancialLvl3Form, "")).toFuture
  }

  def submitAuxiliaryFinancialLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AuxiliaryFinancialLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AuxiliaryFinancialLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadAuxiliaryInsuranceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AuxiliaryInsuranceLvl4Page(AuxiliaryInsuranceLvl4Form, "")).toFuture
  }

  def submitAuxiliaryInsuranceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AuxiliaryInsuranceLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AuxiliaryInsuranceLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadAuxiliaryNonInsuranceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(AuxiliaryNonInsuranceLvl4Page(AuxiliaryNonInsuranceLvl4Form, "")).toFuture
  }

  def submitAuxiliaryNonInsuranceLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    AuxiliaryNonInsuranceLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(AuxiliaryNonInsuranceLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadFinanceInsuranceLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FinanceInsuranceLvl2Page(FinanceInsuranceLvl2Form, "")).toFuture
  }

  def submitFinanceInsuranceLvl2Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FinanceInsuranceLvl2Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FinanceInsuranceLvl2Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadFinancialServicesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(FinancialServicesLvl3Page(FinancialServicesLvl3Form, "")).toFuture
  }

  def submitFinancialServicesLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    FinancialServicesLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(FinancialServicesLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

  def loadHoldingCompaniesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(HoldingCompaniesLvl4Page(HoldingCompaniesLvl4Form, "")).toFuture
  }

  def submitHoldingCompaniesLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    HoldingCompaniesLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(HoldingCompaniesLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadTrustsFundsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(TrustsFundsLvl4Page(TrustsFundsLvl4Form, "")).toFuture
  }

  def submitTrustsFundsLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    TrustsFundsLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(TrustsFundsLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadOtherFinancialLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(OtherFinancialLvl4Page(OtherFinancialLvl4Form, "")).toFuture
  }

  def submitOtherFinancialLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    OtherFinancialLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(OtherFinancialLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadMonetaryIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(MonetaryIntermediationLvl4Page(MonetaryIntermediationLvl4Form, "")).toFuture
  }

  def submitMonetaryIntermediationLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    MonetaryIntermediationLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(MonetaryIntermediationLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }

  def loadInsuranceTypeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(InsuranceTypeLvl4Page(InsuranceTypeLvl4Form, "")).toFuture
  }

  def submitInsuranceTypeLvl4Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    InsuranceTypeLvl4Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(InsuranceTypeLvl4Page(formWithErrors, "")).toFuture,
        form => {
          store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
          Redirect(navigator.nextPage(form.value, "")).toFuture
        }
      )
  }
  def loadInsuranceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(InsuranceLvl3Page(InsuranceLvl3Form, "")).toFuture
  }

  def submitInsuranceLvl3Page(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    InsuranceLvl3Form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(InsuranceLvl3Page(formWithErrors, "")).toFuture,
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

            if (previousAnswer.equals(form.value) && journey.mode.equals(appConfig.NewRegChangeMode))
              Redirect(navigator.nextPage(lvl4Answer, appConfig.NewRegChangeMode)).toFuture
            else {
              store.update[UndertakingJourney](_.setUndertakingSector(Sector.withName(form.value).id))
              store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
              Redirect(navigator.nextPage(form.value, appConfig.NewRegMode)).toFuture
            }
          }
        }
      )
  }

}
