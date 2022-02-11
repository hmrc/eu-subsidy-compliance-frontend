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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Undertaking, UndertakingSubsidies}

case class FinancialDashboardSummary(
  overall: OverallSummary
)

// TODO - do we need to handle dynamic sector limits?
case class OverallSummary(
  startYear: Int,
  endYear: Int,
  // TODO - we can use positive subsidy amount here
  hmrcSubsidyTotal: BigDecimal,
  nonHmrcSubsidyTotal: BigDecimal,
  sector: Sector,
  sectorCap: BigDecimal, // TODO - should we also include the sector?
  allowanceRemaining: BigDecimal
)

object FinancialDashboardSummary {

  // TODO - we can take an optional value from the undertaking. If this isn't present we
  //        should fallback to a default and warn.
  //        the default should reflect the appropriate cap for each sector
  val SectorCap = BigDecimal(200000.00).setScale(2)

  def fromUndertakingSubsidies(u: Undertaking, s: UndertakingSubsidies, startYear: Int, endYear: Int): FinancialDashboardSummary = {
    val overallSummary = OverallSummary(
      startYear = startYear,
      endYear = endYear,
      hmrcSubsidyTotal = s.hmrcSubsidyTotalEUR.setScale(2),
      nonHmrcSubsidyTotal = s.nonHMRCSubsidyTotalEUR.setScale(2),
      sector = u.industrySector,
      sectorCap = u.industrySectorLimit.map(_.setScale(2)).getOrElse(SectorCap),
      allowanceRemaining = BigDecimal(200000.00).setScale(2) - s.hmrcSubsidyTotalEUR - s.nonHMRCSubsidyTotalEUR
    )

    FinancialDashboardSummary(
      overall = overallSummary
    )

  }
}