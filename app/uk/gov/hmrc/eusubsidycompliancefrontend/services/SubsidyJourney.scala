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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, NonHmrcSubsidy, OptionalEORI, OptionalTraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._

case class SubsidyJourney(
  reportPayment: ReportPaymentFormPage = ReportPaymentFormPage(),
  claimDate: ClaimDateFormPage = ClaimDateFormPage(),
  claimAmount: ClaimAmountFormPage = ClaimAmountFormPage(),
  addClaimEori: AddClaimEoriFormPage = AddClaimEoriFormPage(),
  publicAuthority: PublicAuthorityFormPage = PublicAuthorityFormPage(),
  traderRef: TraderRefFormPage = TraderRefFormPage(),
  cya: CyaFormPage = CyaFormPage(),
  existingTransactionId: Option[SubsidyRef] = None,
) extends Journey {

  override protected def steps: List[FormPageBase[_]] = List(
    reportPayment,
    claimDate,
    claimAmount,
    addClaimEori,
    publicAuthority,
    traderRef,
    cya
  )

  def isAmend: Boolean = existingTransactionId.nonEmpty
}

object SubsidyJourney {
  import Journey._ // N.B. don't let intellij delete this

  implicit val formPageClaimDateFormat: OFormat[FormPage[DateFormValues]] = Json.format[FormPage[DateFormValues]]
  implicit val formPageOptionalEORIFormat: OFormat[FormPage[OptionalEORI]] = Json.format[FormPage[OptionalEORI]]
  implicit val formPageOptionalTraderRefFormat: OFormat[FormPage[OptionalTraderRef]] =
    Json.format[FormPage[OptionalTraderRef]]
  implicit val formPageTraderRefFormat: OFormat[FormPage[TraderRef]] = Json.format[FormPage[TraderRef]]
  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def fromNonHmrcSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney =
    SubsidyJourney(
      reportPayment = ReportPaymentFormPage(true.some),
      claimDate = ClaimDateFormPage(DateFormValues.fromDate(nonHmrcSubsidy.allocationDate).some),
      claimAmount = ClaimAmountFormPage(nonHmrcSubsidy.nonHMRCSubsidyAmtEUR.some),
      addClaimEori = AddClaimEoriFormPage(getAddClaimEORI(nonHmrcSubsidy.businessEntityIdentifier).some),
      publicAuthority= PublicAuthorityFormPage(nonHmrcSubsidy.publicAuthority.orElse("".some)),
      traderRef = TraderRefFormPage(getAddTraderRef(nonHmrcSubsidy.traderReference).some),
      existingTransactionId = nonHmrcSubsidy.subsidyUsageTransactionID
    )

  private def getAddClaimEORI(eoriOpt: Option[EORI]): OptionalEORI = {
    eoriOpt.fold(OptionalEORI("false", eoriOpt))(e => OptionalEORI("true", e.some))
  }

  private def getAddTraderRef(traderRefOpt: Option[TraderRef]) =
    traderRefOpt.fold(OptionalTraderRef("false", None))(t => OptionalTraderRef("true", t.some))

  object FormUrls {
    val ReportPayment = "claims"
    val ClaimDateValues = "add-claim-date"
    val ClaimAmount = "add-claim-amount"
    val AddClaimEori = "add-claim-eori"
    val PublicAuthority = "add-claim-public-authority"
    val TraderReference = "add-claim-reference"
    val Cya = "check-your-answers-subsidy"
  }

  object Forms {
    // TODO - replace uris with routes lookups
    case class ReportPaymentFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.ReportPayment }
    case class ClaimDateFormPage(value: Form[DateFormValues] = None) extends FormPageBase[DateFormValues] { val uri = FormUrls.ClaimDateValues }
    case class ClaimAmountFormPage(value: Form[BigDecimal] = None) extends FormPageBase[BigDecimal] { val uri = FormUrls.ClaimAmount }
    case class AddClaimEoriFormPage(value: Form[OptionalEORI] = None) extends FormPageBase[OptionalEORI] { val uri = FormUrls.AddClaimEori }
    case class PublicAuthorityFormPage(value: Form[String] = None) extends FormPageBase[String] { val uri = FormUrls.PublicAuthority }
    case class TraderRefFormPage(value: Form[OptionalTraderRef] = None) extends FormPageBase[OptionalTraderRef] { val uri = FormUrls.TraderReference }
    case class CyaFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.Cya }

    object ReportPaymentFormPage { implicit val reportPaymentFormPageFormat: OFormat[ReportPaymentFormPage] = Json.format }
    object ClaimDateFormPage { implicit val claimDateFormPageFormat: OFormat[ClaimDateFormPage] = Json.format }
    object ClaimAmountFormPage { implicit val claimAmountFormPageFormat: OFormat[ClaimAmountFormPage] = Json.format }
    object AddClaimEoriFormPage { implicit val claimAmountFormPageFormat: OFormat[AddClaimEoriFormPage] = Json.format }
    object PublicAuthorityFormPage { implicit val claimAmountFormPageFormat: OFormat[PublicAuthorityFormPage] = Json.format }
    object TraderRefFormPage { implicit val claimAmountFormPageFormat: OFormat[TraderRefFormPage] = Json.format }
    object CyaFormPage { implicit val claimAmountFormPageFormat: OFormat[CyaFormPage] = Json.format }
  }

}
