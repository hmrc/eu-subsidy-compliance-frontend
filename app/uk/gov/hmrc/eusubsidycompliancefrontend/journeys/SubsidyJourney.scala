/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.journeys

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json._
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.EUR
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.CheckYourAnswersHelper

import scala.concurrent.Future

case class SubsidyJourney(
  reportPayment: ReportPaymentFormPage = ReportPaymentFormPage(),
  reportedNonCustomSubsidy: ReportedNonCustomSubsidyFormPage = ReportedNonCustomSubsidyFormPage(),
  claimDate: ClaimDateFormPage = ClaimDateFormPage(),
  claimAmount: ClaimAmountFormPage = ClaimAmountFormPage(),
  convertedClaimAmountConfirmation: ConvertedClaimAmountConfirmationPage = ConvertedClaimAmountConfirmationPage(),
  addClaimEori: AddClaimEoriFormPage = AddClaimEoriFormPage(),
  addClaimBusiness: AddClaimBusinessFormPage = AddClaimBusinessFormPage(),
  publicAuthority: PublicAuthorityFormPage = PublicAuthorityFormPage(),
  traderRef: TraderRefFormPage = TraderRefFormPage(),
  cya: CyaFormPage = CyaFormPage(),
  submitted: Option[Boolean] = None,
  existingTransactionId: Option[SubsidyRef] = None
) extends Journey {

  override def steps: Array[FormPage[_]] = Array(
    claimDate,
    claimAmount,
    convertedClaimAmountConfirmation,
    addClaimEori,
    addClaimBusiness,
    publicAuthority,
    traderRef,
    cya
  )

  lazy val isAmend: Boolean = traderRef.value.nonEmpty

  override def next(implicit r: Request[_]): Future[Result] = {
    val cyaVisited = this.cya.value.getOrElse(false)
    val defaultContinueUri = r.uri match {
      case url if url == routes.SubsidyController.postClaimDate.url => claimAmount.uri
      case url if url == routes.SubsidyController.postAddClaimAmount.url =>
        if (shouldSkipCurrencyConversion) addClaimEori.uri else convertedClaimAmountConfirmation.uri
      case url if url == routes.SubsidyController.postConfirmClaimAmount.url => addClaimEori.uri
      case url if url == routes.SubsidyController.postAddClaimEori.url =>
        if (shouldAddNewBusiness) addClaimBusiness.uri else publicAuthority.uri
      case url if url == routes.SubsidyController.postAddClaimPublicAuthority.url => traderRef.uri
      case url if url == routes.SubsidyController.postAddClaimReference.url => cya.uri
      case url if url == routes.SubsidyController.postAddClaimBusiness.url =>
        if (addClaimBusiness.value.contains(true)) publicAuthority.uri else addClaimEori.uri

    }
    val useDefaultContinueUri: Boolean =
      (r.uri == routes.SubsidyController.postAddClaimAmount.url && !claimAmountIsInEuros) ||
        (r.uri == routes.SubsidyController.postAddClaimEori.url && shouldAddNewBusiness)

    Future.successful(
      Redirect(CheckYourAnswersHelper.calculateContinueLink(cyaVisited, defaultContinueUri, useDefaultContinueUri))
    )
  }

  override def previous(implicit request: Request[_]): Journey.Uri = {
    val cyaVisited = this.cya.value.getOrElse(false)
    val backLinkUrl = request.uri match {
      case url if url == cya.uri => routes.SubsidyController.backFromCheckYourAnswers.url
      case url if url == traderRef.uri => publicAuthority.uri
      case url if url == publicAuthority.uri => addClaimEori.uri
      case url if url == addClaimEori.uri =>
        if (claimAmountIsInEuros) claimAmount.uri else convertedClaimAmountConfirmation.uri
      case url if url == claimAmount.uri => claimDate.uri
      case url if url == convertedClaimAmountConfirmation.uri => claimAmount.uri
      case url if url == claimDate.uri => reportedNonCustomSubsidy.uri
      case url if url == addClaimBusiness.uri => addClaimEori.uri
      case url if url == reportedNonCustomSubsidy.uri => reportPayment.uri
      case url if url == reportPayment.uri => routes.AccountController.getAccountPage.url
    }
    val useDefaultBackUri: Boolean = request.uri == cya.uri
    CheckYourAnswersHelper.calculateBackLink(cyaVisited, backLinkUrl, useDefaultBackUri)
  }

  override def isEligibleForStep(implicit r: Request[_]): Boolean =
    if (addClaimEori.isCurrentPage && shouldSkipCurrencyConversion) claimAmount.value.isDefined
    else if (publicAuthority.isCurrentPage && !shouldAddNewBusiness) true
    else super.isEligibleForStep

  // When navigating back or forward we should skip the currency conversion step if the user has already entered a
  // claim amount in Euros.
  private def shouldSkipCurrencyConversion: Boolean = claimAmountIsInEuros

  private def shouldAddNewBusiness = addClaimEori.value.exists(_.addToUndertaking == true)

  def setReportPayment(v: Boolean): SubsidyJourney =
    this.copy(reportPayment = reportPayment.copy(v.some))

  def setClaimAmount(c: ClaimAmount): SubsidyJourney = this.copy(claimAmount = claimAmount.copy(value = c.some))
  def setConvertedClaimAmount(c: ClaimAmount): SubsidyJourney =
    this.copy(convertedClaimAmountConfirmation = convertedClaimAmountConfirmation.copy(value = c.some))
  def setClaimDate(d: DateFormValues): SubsidyJourney = this.copy(claimDate = claimDate.copy(value = d.some))
  def setClaimEori(oe: OptionalClaimEori): SubsidyJourney = this.copy(addClaimEori = addClaimEori.copy(oe.some))
  def setAddBusiness(v: Boolean): SubsidyJourney = this.copy(addClaimBusiness = addClaimBusiness.copy(v.some))
  def setPublicAuthority(a: String): SubsidyJourney = this.copy(publicAuthority = publicAuthority.copy(a.some))
  def setTraderRef(o: OptionalTraderRef): SubsidyJourney = this.copy(traderRef = traderRef.copy(o.some))
  def setCya(v: Boolean): SubsidyJourney = this.copy(cya = cya.copy(v.some))

  def setSubmitted(v: Boolean): SubsidyJourney = this.copy(submitted = Some(v))
  def getReportPayment: Boolean = reportPayment.value.getOrElse(false)
  def getClaimAmount: Option[BigDecimal] = claimAmountToBigDecimal(claimAmount)
  def getConvertedClaimAmount: Option[BigDecimal] = claimAmountToBigDecimal(convertedClaimAmountConfirmation)
  def getClaimEori: Option[EORI] = addClaimEori.value.flatMap(_.value).map(EORI(_))
  def getAddBusiness: Boolean = addClaimBusiness.value.getOrElse(false)
  def getReportedNoCustomSubsidy: Boolean = reportedNonCustomSubsidy.value.getOrElse(false)

  def claimAmountIsInEuros: Boolean = claimAmount.value.map(_.currencyCode).contains(EUR)

  def isSubmitted: Boolean = submitted.getOrElse(false)

  private def claimAmountToBigDecimal(f: FormPage[ClaimAmount]) = f.value.map(a => BigDecimal(a.amount))

}

object SubsidyJourney {

  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  object Forms {

    private val controller = routes.SubsidyController

    case class ReportPaymentFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getReportPayment.url
    }

    case class ReportedNonCustomSubsidyFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getReportedNoCustomSubsidyPage.url
    }

    case class ClaimDateFormPage(value: Form[DateFormValues] = None) extends FormPage[DateFormValues] {
      def uri = controller.getClaimDate.url
    }
    case class ClaimAmountFormPage(value: Form[ClaimAmount] = None) extends FormPage[ClaimAmount] {
      def uri = controller.getClaimAmount.url
    }
    case class ConvertedClaimAmountConfirmationPage(value: Form[ClaimAmount] = None) extends FormPage[ClaimAmount] {
      def uri = controller.getConfirmClaimAmount.url
    }
    case class AddClaimEoriFormPage(value: Form[OptionalClaimEori] = None) extends FormPage[OptionalClaimEori] {
      def uri = controller.getAddClaimEori.url
    }
    case class AddClaimBusinessFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getAddClaimBusiness.url
    }
    case class PublicAuthorityFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getAddClaimPublicAuthority.url
    }
    case class TraderRefFormPage(value: Form[OptionalTraderRef] = None) extends FormPage[OptionalTraderRef] {
      def uri = controller.getAddClaimReference.url
    }
    case class CyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCheckAnswers.url
    }

    object ReportedNonCustomSubsidyFormPage {
      implicit val reportedNonCustomSubsidyPageFormPageFormat: OFormat[ReportedNonCustomSubsidyFormPage] = Json.format
    }
    object ReportPaymentFormPage {
      implicit val reportPaymentFormPageFormat: OFormat[ReportPaymentFormPage] =
        Json.format
    }

    object ClaimDateFormPage { implicit val claimDateFormPageFormat: OFormat[ClaimDateFormPage] = Json.format }
    object ClaimAmountFormPage { implicit val claimAmountFormPageFormat: OFormat[ClaimAmountFormPage] = Json.format }
    object ConvertedClaimAmountConfirmationPage {
      implicit val convertedClaimAmountConfirmationPageFormat: OFormat[ConvertedClaimAmountConfirmationPage] =
        Json.format
    }
    object AddClaimEoriFormPage { implicit val claimAmountFormPageFormat: OFormat[AddClaimEoriFormPage] = Json.format }
    object AddClaimBusinessFormPage {
      implicit val claimBusinessFormPageFormat: OFormat[AddClaimBusinessFormPage] = Json.format
    }
    object PublicAuthorityFormPage {
      implicit val claimAmountFormPageFormat: OFormat[PublicAuthorityFormPage] = Json.format
    }
    object TraderRefFormPage { implicit val claimAmountFormPageFormat: OFormat[TraderRefFormPage] = Json.format }
    object CyaFormPage { implicit val claimAmountFormPageFormat: OFormat[CyaFormPage] = Json.format }

  }

}
