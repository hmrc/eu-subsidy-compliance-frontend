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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.json

import java.time.format.DateTimeFormatter
import java.time._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types._

package object eis {

  val clock: Clock                        = Clock.systemUTC()
  val formatter: DateTimeFormatter        = DateTimeFormatter.ISO_INSTANT
  val oddEisDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM-dd")

  def receiptDate: String = {
    val instant      = Instant.now(clock)
    val withoutNanos = instant.minusNanos(instant.getNano)
    formatter.format(withoutNanos)
  }

  implicit class RichLocalDateTime(in: LocalDateTime) {
    def eisFormat: String =
      formatter.format(in.toInstant(ZoneOffset.UTC).minusNanos(in.getNano))
  }

  // provides response for EIS retrieveUndertaking call
  implicit val eisRetrieveUndertakingResponse: Writes[Undertaking] = new Writes[Undertaking] {
    override def writes(o: Undertaking): JsValue = Json.obj(
      "retrieveUndertakingResponse" -> Json.obj(
        "responseCommon" ->
          ResponseCommon(
            EisStatus.OK,
            EisStatusString("ok"),
            LocalDateTime.now,
            None
          ),
        "responseDetail" -> Json.obj(
          "undertakingReference"      -> o.reference,
          "undertakingName"           -> o.name,
          "industrySector"            -> o.industrySector,
          "industrySectorLimit"       -> o.industrySectorLimit,
          "lastSubsidyUsageUpdt"      -> o.lastSubsidyUsageUpdt.map(_.format(oddEisDateFormat)),
          "undertakingBusinessEntity" -> o.undertakingBusinessEntity
        )
      )
    )
  }

  // provides response for EIS updateUndertaking call
  implicit val eisUpdateUndertakingResponse: Writes[UndertakingRef] = new Writes[UndertakingRef] {
    override def writes(o: UndertakingRef): JsValue = Json.obj(
      "updateUndertakingResponse" -> Json.obj(
        "responseCommon" ->
          ResponseCommon(
            EisStatus.OK,
            EisStatusString("ok"),
            LocalDateTime.now,
            None
          ),
        "responseDetail" -> Json.obj(
          "undertakingReference" -> o
        )
      )
    )
  }

  // provides response for EIS updateSubsidyUsage call
  implicit val eisUpdateSubsidyUsageResponse: Writes[SubsidyUpdate] = new Writes[SubsidyUpdate] {
    override def writes(o: SubsidyUpdate): JsValue = Json.obj(
      "amendUndertakingSubsidyUsageResponse" -> Json.obj(
        "responseCommon" ->
          ResponseCommon(
            EisStatus.OK,
            EisStatusString("Success"),
            LocalDateTime.now,
            None
          ),
        "responseDetail" -> Json.obj(
          "undertakingIdentifier" -> o.undertakingIdentifier
        )
      )
    )
  }

  // formatter for the response from EIS when creating the Undertaking
  implicit val eisCreateUndertakingResponse: Writes[UndertakingRef] = new Writes[UndertakingRef] {
    override def writes(undertakingRef: UndertakingRef): JsValue =
      Json.obj(
        "createUndertakingResponse" -> Json.obj(
          "responseCommon" ->
            ResponseCommon(
              EisStatus.OK,
              EisStatusString("String"),
              LocalDateTime.now,
              None
            ),
          "responseDetail" -> Json.obj(
            "undertakingReference" -> undertakingRef
          )
        )
      )
  }

  // provides response from EIS retrieve subsidies call
  implicit val eisRetrieveUndertakingSubsidiesResponse: Writes[UndertakingSubsidies] =
    new Writes[UndertakingSubsidies] {
      // TODO delete this if we can get the case of subsidyUsageTransactionID aligned in SCP06 & 09
      implicit val nonHmrcSubsidyWrites: Writes[NonHmrcSubsidy] = (
        (JsPath \ "subsidyUsageTransactionID").writeNullable[SubsidyRef] and
          (JsPath \ "allocationDate").write[LocalDate] and
          (JsPath \ "submissionDate").write[LocalDate] and
          (JsPath \ "publicAuthority").writeNullable[String] and
          (JsPath \ "traderReference").writeNullable[TraderRef] and
          (JsPath \ "nonHMRCSubsidyAmtEUR").write[SubsidyAmount] and
          (JsPath \ "businessEntityIdentifier").writeNullable[EORI] and
          (JsPath \ "amendmentType").writeNullable[EisSubsidyAmendmentType]
      )(unlift(NonHmrcSubsidy.unapply))

      override def writes(o: UndertakingSubsidies): JsValue = Json.obj(
        "getUndertakingTransactionResponse" -> Json.obj(
          "responseCommon" ->
            ResponseCommon(
              EisStatus.OK,
              EisStatusString("String"),
              LocalDateTime.now,
              None
            ),
          "responseDetail" -> Json.obj(
            "undertakingIdentifier"  -> o.undertakingIdentifier,
            "nonHMRCSubsidyTotalEUR" -> o.nonHMRCSubsidyTotalEUR,
            "nonHMRCSubsidyTotalGBP" -> o.nonHMRCSubsidyTotalGBP,
            "hmrcSubsidyTotalEUR"    -> o.hmrcSubsidyTotalEUR,
            "hmrcSubsidyTotalGBP"    -> o.hmrcSubsidyTotalGBP,
            "nonHMRCSubsidyUsage"    -> o.nonHMRCSubsidyUsage,
            "hmrcSubsidyUsage"       -> o.hmrcSubsidyUsage
          )
        )
      )
    }

  // convenience reads so we can store a created undertaking
  val undertakingRequestReads: Reads[Undertaking] = new Reads[Undertaking] {
    override def reads(json: JsValue): JsResult[Undertaking] = {
      val businessEntity: BusinessEntity = BusinessEntity(
        (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "idValue").as[EORI],
        true
      )
      JsSuccess(
        Undertaking(
          None,
          (json \ "createUndertakingRequest" \ "requestDetail" \ "undertakingName").as[UndertakingName],
          (json \ "createUndertakingRequest" \ "requestDetail" \ "industrySector").as[Sector],
          None,
          None,
          List(businessEntity)
        )
      )
    }
  }

  // convenience reads so we can store business entity updates
  val businessEntityReads: Reads[BusinessEntity] = new Reads[BusinessEntity] {
    override def reads(json: JsValue): JsResult[BusinessEntity] = JsSuccess(
      BusinessEntity(
        (json \ "businessEntityIdentifier").as[EORI],
        (json \ "leadEORIIndicator").as[Boolean]
      )
    )
  }
}
