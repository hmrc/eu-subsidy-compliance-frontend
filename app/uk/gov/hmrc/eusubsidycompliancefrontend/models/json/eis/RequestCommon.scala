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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.json.eis

import java.util.UUID
import play.api.libs.json.{JsLookupResult, JsResult, JsSuccess, JsValue, Json, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.AcknowledgementRef

case class RequestCommon(
  messageType: String,
  acknowledgementReference: AcknowledgementRef = AcknowledgementRef(UUID.randomUUID().toString.replace("-", ""))
)

object RequestCommon {
  implicit val writes: Writes[RequestCommon] = new Writes[RequestCommon] {
    override def writes(o: RequestCommon): JsValue = Json.obj(
      "originatingSystem" -> "MDTP",
      "receiptDate" -> receiptDate,
      "acknowledgementReference" -> o.acknowledgementReference,
      "messageTypes" -> Json.obj(
        "messageType" -> o.messageType
      ),
      "requestParameters" -> Json.arr(
        Json.obj(
          "paramName" -> "REGIME",
          "paramValue" -> "ES"
        )
      )
    )
  }

  implicit val reads = new Reads[RequestCommon] {
    override def reads(json: JsValue): JsResult[RequestCommon] = {
      val requestCommon: JsLookupResult = json \ "requestCommon"
      val ackRef = (requestCommon \ "acknowledgementReference").as[String]
      val messageType = (requestCommon \ "messageTypes" \ "messageType").as[String]
      JsSuccess(RequestCommon(messageType, AcknowledgementRef(ackRef)))
    }
  }
}
