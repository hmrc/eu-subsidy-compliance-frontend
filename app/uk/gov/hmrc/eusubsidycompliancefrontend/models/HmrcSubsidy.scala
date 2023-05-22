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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{DeclarationID, EORI, SubsidyAmount, TaxType, TraderRef}

import java.time.LocalDate

case class HmrcSubsidy(
  declarationID: DeclarationID,
  issueDate: Option[LocalDate],
  acceptanceDate: LocalDate,
  declarantEORI: EORI, // n.b. SCP09 uses looser validation but will stick with ours
  consigneeEORI: EORI,
  taxType: Option[TaxType],
  hmrcSubsidyAmtGBP: Option[SubsidyAmount],
  hmrcSubsidyAmtEUR: Option[SubsidyAmount],
  tradersOwnRefUCR: Option[TraderRef]
)

object HmrcSubsidy {
  implicit val format: Format[HmrcSubsidy] = Json.format[HmrcSubsidy]
}
