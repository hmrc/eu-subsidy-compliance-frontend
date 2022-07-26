/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.EUR
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import java.net.URI
import scala.concurrent.Future
import scala.util.Try

case class SubsidyJourney(
  reportPayment: ReportPaymentFormPage = ReportPaymentFormPage(),
  claimDate: ClaimDateFormPage = ClaimDateFormPage(),
  claimAmount: ClaimAmountFormPage = ClaimAmountFormPage(),
  convertedClaimAmountConfirmation: ConvertedClaimAmountConfirmationPage = ConvertedClaimAmountConfirmationPage(),
  addClaimEori: AddClaimEoriFormPage = AddClaimEoriFormPage(),
  publicAuthority: PublicAuthorityFormPage = PublicAuthorityFormPage(),
  traderRef: TraderRefFormPage = TraderRefFormPage(),
  cya: CyaFormPage = CyaFormPage(),
  existingTransactionId: Option[SubsidyRef] = None
) extends Journey {

  override def steps: Array[FormPage[_]] = Array(
    reportPayment,
    claimDate,
    claimAmount,
    convertedClaimAmountConfirmation,
    addClaimEori,
    publicAuthority,
    traderRef,
    cya
  )

  def isAmend: Boolean = {
    println(s"isAmend: ${existingTransactionId.nonEmpty}")
    existingTransactionId.nonEmpty
  }

  // TODO - review this and simplify where possible
  override def next(implicit r: Request[_]): Future[Result] =
    if (isAmend && shouldSkipCurrencyConversion) {
      println(s"on amend journey - redirecting to check answers page")
      Redirect(routes.SubsidyController.getCheckAnswers()).toFuture
    }
    // TODO - cover this case
    else if (isAmend && claimAmount.isCurrentPage && !shouldSkipCurrencyConversion) {
      println(s"SubsidyJourney: user has entered GBP amount on amend journey - redirecting to confirmation page")
      Redirect(routes.SubsidyController.getConfirmClaimAmount()).toFuture
    }
    // TODO - cover this case
    else if (isAmend && convertedClaimAmountConfirmation.isCurrentPage) {
      println(s"SubsidyJourney - user has confirmed GBP-EUR conversion on amend - redirecting to check answers")
      Redirect(routes.SubsidyController.getCheckAnswers()).toFuture
    }
    else if (claimAmount.isCurrentPage && shouldSkipCurrencyConversion) {
      println(s"user submitted an EUR claim amount - skipping exchange rate")
      Redirect(routes.SubsidyController.getAddClaimEori()).toFuture
    }
    else {
      println(s"no special behaviour needed - deferring to standard next method")
      super.next
    }

  // TODO - review and simplify where possible
  override def previous(implicit request: Request[_]): Journey.Uri = {
    if (reportPayment.isCurrentPage)
      routes.AccountController.getAccountPage().url
    else if (!isAmend && addClaimEori.isCurrentPage && shouldSkipCurrencyConversion)
      claimAmount.uri
    else if (isAmend && convertedClaimAmountConfirmation.isCurrentPage) {
      claimAmount.uri
    }
    else if (isAmend && !cya.isCurrentPage) {
      println(s"previous: On amend journey and request did not originate from CYA so setting back to CYA")
      cya.uri
    }
    else if (isAmend) {
      println(s"On amend journey - Request has referer: ${request.headers.get("Referer")}")

      extractAndParseRefererUrl
        .getOrElse(routes.SubsidyController.getReportPayment().url)
    }
    else {
      println("previous: no other cases triggered - delegating to super.previous")
      super.previous
    }
  }

  private def extractAndParseRefererUrl(implicit request: Request[_]): Option[String] =
    request
      .headers.get("Referer")
      .flatMap { u =>
        Try(URI.create(u))
          .fold(_ => None, uri => Some(uri.getPath))
      }
      .flatMap(getStepWithPath)
      .map(_.uri)

  // TODO - explicit test coverage for this
  override def isEligibleForStep(implicit r: Request[_]): Boolean =
    if (addClaimEori.isCurrentPage && shouldSkipCurrencyConversion) claimAmount.value.isDefined
    else super.isEligibleForStep

  // When navigating back or forward we should skip the currency conversion step if the user has already entered a
  // claim amount in Euros.
  private def shouldSkipCurrencyConversion: Boolean =
    claimAmount.value.map(_.currencyCode).contains(EUR)

  def setReportPayment(v: Boolean): SubsidyJourney = this.copy(reportPayment = reportPayment.copy(value = v.some))
  def setClaimAmount(c: ClaimAmount): SubsidyJourney = this.copy(claimAmount = claimAmount.copy(value = c.some))
  def setConvertedClaimAmount(c: ClaimAmount): SubsidyJourney =
    this.copy(convertedClaimAmountConfirmation = convertedClaimAmountConfirmation.copy(value = c.some))
  def setClaimDate(d: DateFormValues): SubsidyJourney = this.copy(claimDate = claimDate.copy(value = d.some))
  def setClaimEori(oe: OptionalEORI): SubsidyJourney = this.copy(addClaimEori = addClaimEori.copy(oe.some))
  def setPublicAuthority(a: String): SubsidyJourney = this.copy(publicAuthority = publicAuthority.copy(a.some))
  def setTraderRef(o: OptionalTraderRef): SubsidyJourney = this.copy(traderRef = traderRef.copy(o.some))
  def setCya(v: Boolean): SubsidyJourney = this.copy(cya = cya.copy(v.some))

}

object SubsidyJourney {

  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def fromNonHmrcSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney =
    SubsidyJourney(
      reportPayment = ReportPaymentFormPage(true.some),
      claimDate = ClaimDateFormPage(DateFormValues.fromDate(nonHmrcSubsidy.allocationDate).some),
      // TODO - review usage here - need to check claim amount is populated correctly for GBP and EUR cases
      claimAmount = ClaimAmountFormPage(ClaimAmount(EUR, nonHmrcSubsidy.nonHMRCSubsidyAmtEUR.toString()).some),
      addClaimEori = AddClaimEoriFormPage(getAddClaimEORI(nonHmrcSubsidy.businessEntityIdentifier).some),
      publicAuthority = PublicAuthorityFormPage(nonHmrcSubsidy.publicAuthority.orElse("".some)),
      traderRef = TraderRefFormPage(getAddTraderRef(nonHmrcSubsidy.traderReference).some),
      existingTransactionId = nonHmrcSubsidy.subsidyUsageTransactionId
    )

  private def getAddClaimEORI(eoriOpt: Option[EORI]): OptionalEORI =
    eoriOpt.fold(OptionalEORI("false", eoriOpt))(e => OptionalEORI("true", e.some))

  private def getAddTraderRef(traderRefOpt: Option[TraderRef]) =
    traderRefOpt.fold(OptionalTraderRef("false", None))(t => OptionalTraderRef("true", t.some))

  object Forms {

    private val controller = routes.SubsidyController

    case class ReportPaymentFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getReportPayment().url
    }
    case class ClaimDateFormPage(value: Form[DateFormValues] = None) extends FormPage[DateFormValues] {
      def uri = controller.getClaimDate().url
    }
    case class ClaimAmountFormPage(value: Form[ClaimAmount] = None) extends FormPage[ClaimAmount] {
      def uri = controller.getClaimAmount().url
    }
    case class ConvertedClaimAmountConfirmationPage(value: Form[ClaimAmount] = None) extends FormPage[ClaimAmount] {
      def uri = controller.getConfirmClaimAmount().url
    }
    case class AddClaimEoriFormPage(value: Form[OptionalEORI] = None) extends FormPage[OptionalEORI] {
      def uri = controller.getAddClaimEori().url
    }
    case class PublicAuthorityFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getAddClaimPublicAuthority().url
    }
    case class TraderRefFormPage(value: Form[OptionalTraderRef] = None) extends FormPage[OptionalTraderRef] {
      def uri = controller.getAddClaimReference().url
    }
    case class CyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCheckAnswers().url
    }

    object ReportPaymentFormPage {
      implicit val reportPaymentFormPageFormat: OFormat[ReportPaymentFormPage] = Json.format
    }
    object ClaimDateFormPage { implicit val claimDateFormPageFormat: OFormat[ClaimDateFormPage] = Json.format }
    object ClaimAmountFormPage { implicit val claimAmountFormPageFormat: OFormat[ClaimAmountFormPage] = Json.format }
    object ConvertedClaimAmountConfirmationPage { implicit val convertedClaimAmountConfirmationPageFormat: OFormat[ConvertedClaimAmountConfirmationPage] = Json.format }
    object AddClaimEoriFormPage { implicit val claimAmountFormPageFormat: OFormat[AddClaimEoriFormPage] = Json.format }
    object PublicAuthorityFormPage {
      implicit val claimAmountFormPageFormat: OFormat[PublicAuthorityFormPage] = Json.format
    }
    object TraderRefFormPage { implicit val claimAmountFormPageFormat: OFormat[TraderRefFormPage] = Json.format }
    object CyaFormPage { implicit val claimAmountFormPageFormat: OFormat[CyaFormPage] = Json.format }

  }

}
