/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

case class BeneficiaryIDResponse(processingDate: Option[String], beneficiaryInfo: Option[Seq[BeneficiaryInfoResp]])

object BeneficiaryIDResponse {
  implicit val reads: Reads[BeneficiaryIDResponse] = (
    (JsPath \ "processingDate").readNullable[String] and
      (JsPath \ "beneficiaryInfo").readNullable[Seq[BeneficiaryInfoResp]]
  )(BeneficiaryIDResponse.apply _)
  implicit val writes: OWrites[BeneficiaryIDResponse] = Json.writes[BeneficiaryIDResponse]
}

case class BeneficiaryInfoResp(
  eori: Option[String],
  benName: Option[String],
  benIDType: Option[String],
  benIDValue: Option[String],
  validated: Option[Boolean]
)

object BeneficiaryInfoResp {
  implicit val reads: Reads[BeneficiaryInfoResp] = (
    (JsPath \ "eori").readNullable[String] and
      (JsPath \ "benName").readNullable[String] and
      (JsPath \ "benIDType").readNullable[String] and
      (JsPath \ "benIDValue").readNullable[String] and
      (JsPath \ "validated").readNullable[Boolean]
  )(BeneficiaryInfoResp.apply _)
  implicit val writes: OWrites[BeneficiaryInfoResp] = Json.writes[BeneficiaryInfoResp]
}
