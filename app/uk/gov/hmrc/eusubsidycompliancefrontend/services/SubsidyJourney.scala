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

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, NonHmrcSubsidy, OptionalEORI, OptionalTraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

case class SubsidyJourney(
  reportPayment: ReportPaymentFormPage = ReportPaymentFormPage(),
  claimDate: ClaimDateFormPage = ClaimDateFormPage(),
  claimAmount: ClaimAmountFormPage = ClaimAmountFormPage(),
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
    addClaimEori,
    publicAuthority,
    traderRef,
    cya
  )

  def isAmend: Boolean = existingTransactionId.nonEmpty

  override def next(implicit r: Request[_]): Future[Result] =
    if (isAmend) Redirect(routes.SubsidyController.getCheckAnswers()).toFuture
    else super.next

  override def previous(implicit request: Request[_]): Journey.Uri = if (
    request.uri == routes.SubsidyController.getReportPayment().url
  ) routes.AccountController.getAccountPage().url
  else super.previous

  def setReportPayment(v: Boolean): SubsidyJourney = this.copy(reportPayment = reportPayment.copy(value = v.some))
  def setClaimAmount(b: String): SubsidyJourney = this.copy(claimAmount = claimAmount.copy(value = b.some))
  def setClaimDate(d: DateFormValues): SubsidyJourney = this.copy(claimDate = claimDate.copy(value = d.some))
  def setClaimEori(oe: OptionalEORI): SubsidyJourney = this.copy(addClaimEori = addClaimEori.copy(oe.some))
  def setPublicAuthority(a: String): SubsidyJourney = this.copy(publicAuthority = publicAuthority.copy(a.some))
  def setTraderRef(o: OptionalTraderRef): SubsidyJourney = this.copy(traderRef = traderRef.copy(o.some))
  def setCya(v: Boolean): SubsidyJourney = this.copy(cya = cya.copy(v.some))

}

object SubsidyJourney {
  val claimAmountPrefix = "€"

  def isClaimAmountPrefixEuros(amountEntered: String) = amountEntered.take(1) === claimAmountPrefix

  def getValidClaimAmount(amountEntered: String) =
    if (isClaimAmountPrefixEuros(amountEntered)) amountEntered.drop(1) else amountEntered

  val claimAmountPrefix = "€"

  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def isClaimAmountPrefixEuros(amountEntered: String) = amountEntered.take(1) === claimAmountPrefix

  def trimAmount(amount: String) = amount.trim.replaceAll(",", "").replaceAll("\\s", "")

  def getValidClaimAmount(amountEntered: String) =
    if (isClaimAmountPrefixEuros(amountEntered)) trimAmount(amountEntered.drop(1)) else trimAmount(amountEntered)

  def fromNonHmrcSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney =
    SubsidyJourney(
      reportPayment = ReportPaymentFormPage(true.some),
      claimDate = ClaimDateFormPage(DateFormValues.fromDate(nonHmrcSubsidy.allocationDate).some),
      claimAmount = ClaimAmountFormPage(nonHmrcSubsidy.nonHMRCSubsidyAmtEUR.toString().some),
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
    case class ClaimAmountFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getClaimAmount().url
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
    object AddClaimEoriFormPage { implicit val claimAmountFormPageFormat: OFormat[AddClaimEoriFormPage] = Json.format }
    object PublicAuthorityFormPage {
      implicit val claimAmountFormPageFormat: OFormat[PublicAuthorityFormPage] = Json.format
    }
    object TraderRefFormPage { implicit val claimAmountFormPageFormat: OFormat[TraderRefFormPage] = Json.format }
    object CyaFormPage { implicit val claimAmountFormPageFormat: OFormat[CyaFormPage] = Json.format }

  }

}
