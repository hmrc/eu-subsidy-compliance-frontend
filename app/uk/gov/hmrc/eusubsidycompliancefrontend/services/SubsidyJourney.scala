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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.FormUrls._

case class SubsidyJourney(
   reportPayment: FormPage[Boolean] = FormPage(ReportPayment),
   claimDate: FormPage[DateFormValues] = FormPage(ClaimDateValues),
   claimAmount: FormPage[BigDecimal] = FormPage(ClaimAmount),
   addClaimEori: FormPage[OptionalEORI] = FormPage(AddClaimEori),
   publicAuthority: FormPage[String] = FormPage(PublicAuthority),
   traderRef: FormPage[OptionalTraderRef] = FormPage(TraderReference),
   cya: FormPage[Boolean] = FormPage(Cya),
   existingTransactionId: Option[SubsidyRef] = None,

) extends Journey {

  override protected def steps: List[FormPage[_]] = List(
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
  implicit val formPageOptionalTraderRefFormat: OFormat[FormPage[OptionalTraderRef]] = Json.format[FormPage[OptionalTraderRef]]
  implicit val formPageTraderRefFormat: OFormat[FormPage[TraderRef]] = Json.format[FormPage[TraderRef]]
  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def fromNonHmrcSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney = {
    val newJourney = SubsidyJourney()
    newJourney
      .copy(
        reportPayment = newJourney.reportPayment.copy(value = Some(true)),
        claimDate = newJourney.claimDate.copy(value = Some(DateFormValues.fromDate(nonHmrcSubsidy.allocationDate))),
        claimAmount = newJourney.claimAmount.copy(value = Some(nonHmrcSubsidy.nonHMRCSubsidyAmtEUR)),
        addClaimEori = newJourney.addClaimEori.copy(value =  getAddClaimEORI(nonHmrcSubsidy.businessEntityIdentifier).some),
        publicAuthority = newJourney.publicAuthority.copy(value = Some(nonHmrcSubsidy.publicAuthority.getOrElse(""))),
        traderRef = newJourney.traderRef.copy(value = getAddTraderRef(nonHmrcSubsidy.traderReference).some),
        existingTransactionId = nonHmrcSubsidy.subsidyUsageTransactionID
    )
  }

  private def getAddClaimEORI(eoriOpt: Option[EORI]) = if(eoriOpt.isDefined) OptionalEORI("true", eoriOpt) else OptionalEORI("false", eoriOpt)
  private def getAddTraderRef(traderRefOpt: Option[TraderRef]) = if(traderRefOpt.isDefined) OptionalTraderRef("true", traderRefOpt) else OptionalTraderRef("false", traderRefOpt)

  object FormUrls {
    val ReportPayment = "claims"
    val ClaimDateValues = "add-claim-date"
    val ClaimAmount = "add-claim-amount"
    val AddClaimEori = "add-claim-eori"
    val PublicAuthority= "add-claim-public-authority"
    val TraderReference = "add-claim-reference"
    val Cya = "check-your-answers-subsidy"
  }

}
