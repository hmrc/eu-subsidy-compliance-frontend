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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import play.api.libs.json.{Format, Json, OFormat}

import java.time.LocalDateTime

final case class EmailAddress(value: String) extends AnyVal

object EmailAddress {
  implicit val format: Format[EmailAddress] = Json.valueFormat[EmailAddress]
}

case class Undeliverable(eventId: String)

object Undeliverable {
  implicit val format: OFormat[Undeliverable] = Json.format[Undeliverable]
}

case class EmailAddressResponse(
  address: EmailAddress,
  timestamp: Option[LocalDateTime],
  undeliverable: Option[Undeliverable]
)

object EmailAddressResponse {
  implicit val format: OFormat[EmailAddressResponse] = Json.format[EmailAddressResponse]
}
