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
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, NonHmrcSubsidy, OptionalEORI, OptionalTraderRef}

case class SubsidyJourney(
   reportPayment: FormPage[Boolean] = FormPage("claims"),
   claimDate: FormPage[DateFormValues] = FormPage("add-claim-date"),
   claimAmount: FormPage[BigDecimal] = FormPage("add-claim-amount"),
   addClaimEori: FormPage[OptionalEORI] = FormPage("add-claim-eori"),
   publicAuthority: FormPage[String] = FormPage("add-claim-public-authority"),
   traderRef: FormPage[OptionalTraderRef] = FormPage("add-claim-reference"),
   cya: FormPage[Boolean] = FormPage("check-your-answers-subsidy"),
   existingTransactionId: Option[SubsidyRef] = None,

) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    SubsidyJourney.
      unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])

  def isAmend: Boolean = existingTransactionId.nonEmpty
}

object SubsidyJourney {
  import Journey._ // N.B. don't let intellij delete this

  implicit val formPageClaimDateFormat: OFormat[FormPage[DateFormValues]] =
    Json.format[FormPage[DateFormValues]]


  implicit val formPageOptionalEORIFormat: OFormat[FormPage[OptionalEORI]] =
    Json.format[FormPage[OptionalEORI]]

  implicit val formPageOptionalTraderRefFormat: OFormat[FormPage[OptionalTraderRef]] =
    Json.format[FormPage[OptionalTraderRef]]

  implicit val formPageTraderRefFormat: OFormat[FormPage[TraderRef]] =
    Json.format[FormPage[TraderRef]]

  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def fromNonHmrcSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney = {
    val newJourney = SubsidyJourney()
    newJourney
      .copy(
        reportPayment = newJourney.reportPayment.copy(value = Some(true)),
        claimDate = newJourney.claimDate.copy(value = Some(DateFormValues.fromDate(nonHmrcSubsidy.submissionDate))),
        claimAmount = newJourney.claimAmount.copy(value = Some(nonHmrcSubsidy.nonHMRCSubsidyAmtEUR)),
        addClaimEori = newJourney.addClaimEori.copy(value =  getAddClaimEORI(nonHmrcSubsidy.businessEntityIdentifier).some),
        publicAuthority = newJourney.publicAuthority.copy(value = Some(nonHmrcSubsidy.publicAuthority.getOrElse(""))),
        traderRef = newJourney.traderRef.copy(value = getAddTraderRef(nonHmrcSubsidy.traderReference).some),
//        traderRef = newJourney.traderRef.copy(value = Some(nonHmrcSubsidy.traderReference)),
        existingTransactionId = nonHmrcSubsidy.subsidyUsageTransactionID
    )
  }

  private def getAddClaimEORI(eoriOpt: Option[EORI]) = if(eoriOpt.isDefined) OptionalEORI("true", eoriOpt) else OptionalEORI("false", eoriOpt)
  private def getAddTraderRef(traderRefOpt: Option[TraderRef]) = if(traderRefOpt.isDefined) OptionalTraderRef("true", traderRefOpt) else OptionalTraderRef("false", traderRefOpt)
}
