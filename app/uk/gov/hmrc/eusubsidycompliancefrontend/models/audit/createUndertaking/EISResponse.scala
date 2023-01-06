/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking

import play.api.libs.json.{Json, Writes}

case class EISResponse(createUndertakingResponse: CreateUndertakingResponse)

object EISResponse {
  implicit val writes: Writes[EISResponse] = Json.writes
}
final case class CreateUndertakingResponse(responseCommon: ResponseCommonUndertaking, responseDetails: ResponseDetail)

object CreateUndertakingResponse {
  implicit val writes: Writes[CreateUndertakingResponse] = Json.writes
}
