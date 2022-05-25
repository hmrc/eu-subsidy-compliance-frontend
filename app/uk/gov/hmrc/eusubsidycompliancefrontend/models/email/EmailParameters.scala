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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.email

import play.api.libs.json.Json
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}

case class EmailParameters(
  eori: EORI,
  beEORI: Option[EORI], // TODO - verify that options are rendered correctly
  undertakingName: UndertakingName,
  undertakingRef: UndertakingRef,
  effectiveDate: Option[String], // TODO - verify that options are rendered correctly
  description: String, // TODO - how is this field used? seems to be a proxy for template name
)

object EmailParameters {
  implicit val format = Json.format[EmailParameters]
}
