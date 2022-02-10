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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.models

import uk.gov.hmrc.eusubsidycompliancefrontend.models.UndertakingSubsidies

case class FinancialDashboardSummary(
  overall: OverallSummary
)

// TODO - do we need to handle dynamic sector limits?
case class OverallSummary(
  startYear: Int,
  endYear: Int,
  hmrcSubsidyTotal: BigDecimal,
  nonHmrcSubsidyTotal: BigDecimal,
  sectorCap: BigDecimal, // TODO - should we also include the sector?
  allowanceRemaining: BigDecimal
)

object FinancialDashboardSummary {

  // TODO - where is this defined and are there different values for different sectors?
  val SectorCap = BigDecimal(200000.00)

  def fromUndertakingSubsidies(u: UndertakingSubsidies, startYear: Int, endYear: Int): FinancialDashboardSummary = {
    val overallSummary = OverallSummary(
      startYear = startYear,
      endYear = endYear,
      hmrcSubsidyTotal = u.hmrcSubsidyTotalEUR,
      nonHmrcSubsidyTotal = u.nonHMRCSubsidyTotalEUR,
      sectorCap = BigDecimal(200000.00),
      allowanceRemaining = BigDecimal(200000.00) - u.hmrcSubsidyTotalEUR - u.nonHMRCSubsidyTotalEUR
    )

    FinancialDashboardSummary(
      overall = overallSummary
    )

  }
}