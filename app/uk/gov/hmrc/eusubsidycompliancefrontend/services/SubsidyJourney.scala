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

import play.api.libs.json._
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, NonHmrcSubsidy}

case class SubsidyJourney(
  reportPayment: FormPage[Boolean] = FormPage("claims"),
  claimDate: FormPage[DateFormValues] = FormPage("add-claim-date"),
  claimAmount: FormPage[BigDecimal] = FormPage("add-claim-amount"),
  addClaimEori: FormPage[Option[EORI]] = FormPage("add-claim-eori"),
  publicAuthority: FormPage[String] = FormPage("add-claim-public-authority"),
  traderRef: FormPage[Option[TraderRef]] = FormPage("add-claim-reference"),
  cya: FormPage[Boolean] = FormPage("check-your-answers-subsidy"),
  existingTransactionId: Option[SubsidyRef] = None
) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    SubsidyJourney.
      unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])

  def isAmend(): Boolean = existingTransactionId.nonEmpty
}

object SubsidyJourney {
  import Journey._ // N.B. don't let intellij delete this

  implicit val formPageClaimDateFormat: OFormat[FormPage[DateFormValues]] =
    Json.format[FormPage[DateFormValues]]

  implicit val formPageAddTraderReferenceFormat: Format[FormPage[Option[TraderRef]]] = new Format[FormPage[Option[TraderRef]]] {

    override def writes(o: FormPage[Option[TraderRef]]): JsValue = {
      val traderRefOpt: JsValue = o.value.flatten match {
        case Some(eori) => JsString(eori)
        case _ => JsNull
      }
      Json.obj(
        "uri" -> o.uri,
        "traderRef" -> traderRefOpt
      )
    }

    override def reads(json: JsValue): JsResult[FormPage[Option[TraderRef]]] = {
      val foo: Option[TraderRef] = (json \ "traderRef").asOpt[TraderRef]
      val bar = FormPage[Option[TraderRef]]("add-trader-ref", Some(foo))
      JsSuccess(bar)
    }
  }

  implicit val formPageAddClaimEoriFormat: Format[FormPage[Option[EORI]]] = new Format[FormPage[Option[EORI]]] {

    override def writes(o: FormPage[Option[EORI]]): JsValue = {
      val eoriOpt: JsValue = o.value.flatten match {
        case Some(eori) => JsString(eori)
        case _ => JsNull
      }
      Json.obj(
        "uri" -> o.uri,
        "claimEori" -> eoriOpt
      )
    }

    override def reads(json: JsValue): JsResult[FormPage[Option[EORI]]] = {
      val foo: Option[EORI] = (json \ "claimEori").asOpt[EORI]
      val bar = FormPage[Option[EORI]]("add-claim-eori", Some(foo))
      JsSuccess(bar)
    }
  }

  implicit val formPageTraderRefFormat: OFormat[FormPage[TraderRef]] =
    Json.format[FormPage[TraderRef]]

  implicit val format: Format[SubsidyJourney] = Json.format[SubsidyJourney]

  def fromSubsidy(nonHmrcSubsidy: NonHmrcSubsidy): SubsidyJourney = {
    val newJourney = SubsidyJourney()
    newJourney
      .copy(reportPayment = newJourney.reportPayment.copy(value = Some(true)))
      .copy(claimDate = newJourney.claimDate.copy(value = Some(DateFormValues.fromDate(nonHmrcSubsidy.submissionDate))))
      .copy(claimAmount = newJourney.claimAmount.copy(value = Some(nonHmrcSubsidy.nonHMRCSubsidyAmtEUR)))
      .copy(addClaimEori = newJourney.addClaimEori.copy(value = Some(nonHmrcSubsidy.businessEntityIdentifier)))
      .copy(publicAuthority = newJourney.publicAuthority.copy(value = Some(nonHmrcSubsidy.publicAuthority.getOrElse(""))))
      .copy(traderRef = newJourney.traderRef.copy(value = Some(nonHmrcSubsidy.traderReference)))
      .copy(existingTransactionId = nonHmrcSubsidy.subsidyUsageTransactionID)
  }
}
