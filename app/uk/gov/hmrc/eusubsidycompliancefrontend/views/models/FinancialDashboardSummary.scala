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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, Sector, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._

import java.time.LocalDate

case class FinancialDashboardSummary(
  overall: OverallSummary,
  taxYears: Seq[TaxYearSummary] = Seq.empty
)

case class OverallSummary(
  startYear: Int,
  endYear: Int,
  hmrcSubsidyTotal: SubsidyAmount,
  nonHmrcSubsidyTotal: SubsidyAmount,
  sector: Sector,
  sectorCap: IndustrySectorLimit
) {
  def total: SubsidyAmount = SubsidyAmount(hmrcSubsidyTotal + nonHmrcSubsidyTotal)
  def allowanceRemaining: SubsidyAmount = SubsidyAmount(sectorCap - total)
}

case class TaxYearSummary(
  startYear: Int,
  hmrcSubsidyTotal: SubsidyAmount,
  nonHmrcSubsidyTotal: SubsidyAmount
) {
  def total: SubsidyAmount = SubsidyAmount(hmrcSubsidyTotal + nonHmrcSubsidyTotal)
  def endYear: Int = startYear + 1
}

object FinancialDashboardSummary {

  // Fallback values should no sector be defined on the undertaking.
  private val DefaultSectorLimits = Map(
    Sector.agriculture -> IndustrySectorLimit(30000.00),
    Sector.aquaculture -> IndustrySectorLimit(20000.00),
    Sector.other -> IndustrySectorLimit(200000.00),
    Sector.transport -> IndustrySectorLimit(100000.00)
  )

  // Generates summarised data to populate the financial dashboard page.
  // All currency amounts in EUR.
  def fromUndertakingSubsidies(
    undertaking: Undertaking,
    subsidies: UndertakingSubsidies,
    startDate: LocalDate,
    endDate: LocalDate
  ): FinancialDashboardSummary = {

    val sectorCapOrDefault: IndustrySectorLimit = undertaking.industrySectorLimit
      .getOrElse(DefaultSectorLimits(undertaking.industrySector))

    val overallSummary = OverallSummary(
      startYear = startDate.getYear,
      endYear = endDate.getYear,
      hmrcSubsidyTotal = SubsidyAmount(subsidies.hmrcSubsidyTotalEUR),
      nonHmrcSubsidyTotal = SubsidyAmount(subsidies.nonHMRCSubsidyTotalEUR),
      sector = undertaking.industrySector,
      sectorCap = sectorCapOrDefault
    )

    // We assume that acceptanceDate is the correct field to use since it's mandatory. There is also an issueDate
    // field but since this is optional it makes grouping by tax year impossible if no value is present.
    // We also assume that the amount field holds a value in EUR.
    def sumByTaxYear(m: Seq[(LocalDate, SubsidyAmount)]): Map[LocalDate, SubsidyAmount] =
      m.groupBy(kv => kv._1).map { case (key, value) =>
        key -> value
          .map(_._2)
          .fold(SubsidyAmount.Zero)((a, b) => SubsidyAmount(a + b))
      }

    val hmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = sumByTaxYear(
      subsidies.hmrcSubsidyUsage
        .map(i => i.acceptanceDate.toTaxYearStart -> i.amount.getOrElse(SubsidyAmount.Zero))
    )

    val nonHmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = sumByTaxYear(
      subsidies.nonHMRCSubsidyUsage
        .map(i => i.allocationDate.toTaxYearStart -> i.nonHMRCSubsidyAmtEUR)
    )

    // Generate summaries for each starting tax year value in descending year order.
    val taxYearSummaries = (endDate.toTaxYearStart.getYear to startDate.getYear by -1)
      .map(_ - startDate.getYear)
      .map(d => LocalDate.from(startDate).plusYears(d))
      .map(d =>
        TaxYearSummary(
          startYear = d.getYear,
          hmrcSubsidyTotal = hmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero),
          nonHmrcSubsidyTotal = nonHmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero)
        )
      )

    FinancialDashboardSummary(
      overall = overallSummary,
      taxYears = taxYearSummaries
    )
  }
}
