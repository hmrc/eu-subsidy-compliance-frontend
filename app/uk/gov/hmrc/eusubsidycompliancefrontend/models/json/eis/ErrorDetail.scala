/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{CorrelationID, ErrorCode, Source, ErrorMessage}

case class ErrorDetail(
  errorCode: ErrorCode,
  errorMessage: ErrorMessage,
  sourceFaultDetail: List[String],
  source: Source = Source("EIS"),
  timestamp: LocalDateTime = LocalDateTime.now,
  correlationId: CorrelationID = CorrelationID(UUID.randomUUID().toString)
)


object ErrorDetail {
  val oddEisFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  implicit val errorDetailWrites: Writes[ErrorDetail] = new Writes[ErrorDetail] {

    override def writes(o: ErrorDetail): JsValue = Json.obj(
      "errorDetail" -> Json.obj(
        "timestamp" -> o.timestamp.format(oddEisFormat),
        "correlationId" -> o.correlationId,
        "errorCode" -> o.errorCode,
        "errorMessage" -> o.errorMessage,
        "source" -> o.source,
        "sourceFaultDetail" -> Json.obj(
          "detail" -> o.sourceFaultDetail
        )
      )
    )
  }
}
