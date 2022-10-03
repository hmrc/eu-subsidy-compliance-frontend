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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{SubsidyAmount, UndertakingRef}

import java.time.LocalDate

case class UndertakingSubsidies(
  undertakingIdentifier: UndertakingRef,
  nonHMRCSubsidyTotalEUR: SubsidyAmount,
  nonHMRCSubsidyTotalGBP: SubsidyAmount,
  hmrcSubsidyTotalEUR: SubsidyAmount,
  hmrcSubsidyTotalGBP: SubsidyAmount,
  nonHMRCSubsidyUsage: List[NonHmrcSubsidy],
  hmrcSubsidyUsage: List[HmrcSubsidy]
) {

  def hasNeverSubmitted: Boolean = nonHMRCSubsidyUsage.isEmpty

  def lastSubmitted: Option[LocalDate] =
      nonHMRCSubsidyUsage
        .sorted(NonHmrcSubsidy.SortOrder.bySubmissionDate)
        .lastOption
        .map(_.submissionDate)

  // For display use cases that require the payments to be ordered by reverse allocation date
  def forReportedPaymentsPage =
    this.copy(
      nonHMRCSubsidyUsage =
        nonHMRCSubsidyUsage
          .sorted(NonHmrcSubsidy.SortOrder.byAllocationDate)
          .reverse
    )

}

object UndertakingSubsidies {
  implicit val format: Format[UndertakingSubsidies] = Json.format[UndertakingSubsidies]
}
