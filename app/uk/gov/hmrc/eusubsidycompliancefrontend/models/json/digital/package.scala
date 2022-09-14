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

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.json.eis.{Params, RequestCommon}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Undertaking, UndertakingBusinessEntityUpdate}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

package object digital {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  implicit val undertakingFormat: Format[Undertaking] = new Format[Undertaking] {

    val requestCommon = RequestCommon(
      "CreateNewUndertaking"
    )

    // provides json for EIS createUndertaking call
    override def writes(o: Undertaking): JsValue = {
      val lead: BusinessEntity =
        o.undertakingBusinessEntity match {
          case h :: Nil => h
          case _ =>
            throw new IllegalStateException(s"unable to create undertaking with missing or multiple business entities")
        }

      Json.obj(
        "createUndertakingRequest" -> Json.obj(
          "requestCommon" -> requestCommon,
          "requestDetail" -> Json.obj(
            "undertakingName" -> o.name,
            "industrySector" -> o.industrySector,
            "businessEntity" ->
              Json.obj(
                "idType" -> "EORI",
                "idValue" -> JsString(lead.businessEntityIdentifier)
              ),
            "undertakingStartDate" -> dateFormatter.format(LocalDate.now)
          )
        )
      )

    }

    // provides Undertaking from EIS retrieveUndertaking response
    override def reads(retrieveUndertakingResponse: JsValue): JsResult[Undertaking] = {
      val responseCommon: JsLookupResult =
        retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case "NOT_OK" =>
          val processingDate = (responseCommon \ "processingDate").as[ZonedDateTime]
          val statusText = (responseCommon \ "statusText").asOpt[String]
          val returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]]
          throw new EisBadResponseException("NOT_OK", processingDate, statusText, returnParameters)
        case "OK" =>
          val responseDetail: JsLookupResult =
            retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseDetail"
          val undertakingRef: UndertakingRef = (responseDetail \ "undertakingReference").as[UndertakingRef]
          val undertakingName: UndertakingName = (responseDetail \ "undertakingName").as[UndertakingName]
          val industrySector: Sector = (responseDetail \ "industrySector").as[Sector]
          val industrySectorLimit: IndustrySectorLimit =
            (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit]
          val lastSubsidyUsageUpdt: LocalDate =
            (responseDetail \ "lastSubsidyUsageUpdt").as[LocalDate](new Reads[LocalDate] {
              override def reads(json: JsValue): JsResult[LocalDate] =
                JsSuccess(LocalDate.parse(json.as[String], eis.oddEisDateFormat))
            })
          val undertakingBusinessEntity: List[BusinessEntity] =
            (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
          JsSuccess(
            Undertaking(
              undertakingRef,
              undertakingName,
              industrySector,
              industrySectorLimit.some,
              lastSubsidyUsageUpdt.some,
              undertakingBusinessEntity
            )
          )
        case _ => JsError("unable to derive Error or Success from SCP04 response")
      }
    }
  }

  // provides json for EIS retrieveUndertaking call
  implicit val retrieveUndertakingEORIWrites: Writes[EORI] = new Writes[EORI] {
    val requestCommon = RequestCommon(
      "RetrieveUndertaking"
    )

    override def writes(o: EORI): JsValue = Json.obj(
      "retrieveUndertakingRequest" -> Json.obj(
        "requestCommon" -> requestCommon,
        "requestDetail" -> Json.obj(
          "idType" -> "EORI",
          "idValue" -> o
        )
      )
    )
  }

  // provides json for EIS Amend Undertaking Member Data (business entities) call
  implicit val amendUndertakingMemberDataWrites: Writes[UndertakingBusinessEntityUpdate] =
    new Writes[UndertakingBusinessEntityUpdate] {
      override def writes(o: UndertakingBusinessEntityUpdate): JsValue = Json.obj(
        "undertakingIdentifier" -> JsString(o.undertakingIdentifier),
        "undertakingComplete" -> JsBoolean(true),
        "memberAmendments" -> o.businessEntityUpdates
      )
    }

  // provides reads for eis response for undertaking create call
  implicit val undertakingCreateResponseReads: Reads[UndertakingRef] = new Reads[UndertakingRef] {
    override def reads(json: JsValue): JsResult[UndertakingRef] = {
      val responseCommon: JsLookupResult = json \ "createUndertakingResponse" \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case "NOT_OK" =>
          val processingDate = (responseCommon \ "processingDate").as[ZonedDateTime]
          val statusText = (responseCommon \ "statusText").asOpt[String]
          val returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]]
          throw new EisBadResponseException("NOT_OK", processingDate, statusText, returnParameters)
        case "OK" =>
          val ref = (json \ "createUndertakingResponse" \ "responseDetail" \ "undertakingReference").as[String]
          JsSuccess(UndertakingRef(ref))
        case _ => JsError("unable to derive Error or Success from SCP02 response")
      }
    }
  }
}
