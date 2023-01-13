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

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, SubsidyRef, TraderRef}

case class NonHmrcSubsidy(
  subsidyUsageTransactionId: Option[SubsidyRef],
  allocationDate: LocalDate,
  submissionDate: LocalDate,
  publicAuthority: Option[String], // this shouldn't be optional, is required in create API but not retrieve
  traderReference: Option[TraderRef],
  nonHMRCSubsidyAmtEUR: SubsidyAmount,
  businessEntityIdentifier: Option[EORI],
  amendmentType: Option[EisSubsidyAmendmentType] = Option.empty, // this only used for create
  removed: Option[Boolean] = Some(false)
) {
  val isRemoved: Boolean = removed.contains(true)
}

object NonHmrcSubsidy {
  implicit val format: OFormat[NonHmrcSubsidy] = Json.format[NonHmrcSubsidy]

  object SortOrder {
    implicit val bySubmissionDate: Ordering[NonHmrcSubsidy] = Ordering.by(_.submissionDate.toEpochDay)
    implicit val byAllocationDate: Ordering[NonHmrcSubsidy] = Ordering.by(_.allocationDate.toEpochDay)
  }
}
