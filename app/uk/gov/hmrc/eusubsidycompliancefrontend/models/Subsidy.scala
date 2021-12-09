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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, PositiveSubsidyAmount, SubsidyRef, TraderRef}

// TODO - hopefully we can delete this - need to get bits of SCP06 and SCP09 aligned
case class Subsidy(
  subsidyUsageTransactionId: Option[SubsidyRef],
  allocationDate: LocalDate,
  submissionDate: LocalDate,
  publicAuthority: String, // no regex
  traderReference: Option[TraderRef], // no regex in create API but one in retrieve API!
  nonHMRCSubsidyAmtEUR: PositiveSubsidyAmount, // TODO consider using sane names and write a bespoke formatter
  businessEntityIdentifier: Option[EORI],
  amendmentType: Option[EisSubsidyAmendmentType] = Option.empty,
)

object Subsidy {
  implicit val format: OFormat[Subsidy] = Json.format[Subsidy]
}