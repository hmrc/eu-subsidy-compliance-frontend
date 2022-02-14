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
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TaxYearSyntax._

import java.time.LocalDate

case class FinancialDashboardSummary(
  overall: OverallSummary,
  taxYears: Seq[TaxYearSummary] = Seq.empty
)

// TODO - provide helper methods or vals
case class OverallSummary(
  startYear: Int,
  endYear: Int,
  hmrcSubsidyTotal: SubsidyAmount,
  nonHmrcSubsidyTotal: SubsidyAmount,
  sector: Sector,
  sectorCap: IndustrySectorLimit,
  allowanceRemaining: SubsidyAmount
)

case class TaxYearSummary(
  year: Int,
  hmrcSubsidyTotal: SubsidyAmount,
  nonHmrcSubsidyTotal: SubsidyAmount,
) {
  // TODO - confirm scale is ok
  val total: SubsidyAmount = SubsidyAmount(hmrcSubsidyTotal + nonHmrcSubsidyTotal)
}

object FinancialDashboardSummary {

  // Fallback values should no value be present on the undertaking.
  private val DefaultSectorLimits = Map(
    Sector.agriculture -> IndustrySectorLimit(30000.00),
    Sector.aquaculture -> IndustrySectorLimit(20000.00),
    Sector.other -> IndustrySectorLimit(200000.00),
    Sector.transport -> IndustrySectorLimit(100000.00),
  )

  // TODO - remove the set scale calls - this could (should?) happen in the view instead via a helper/formatter
  def fromUndertakingSubsidies(
    u: Undertaking,
    s: UndertakingSubsidies,
    startDate: LocalDate,
    endDate: LocalDate
  ): FinancialDashboardSummary = {

    val overallSummary = OverallSummary(
      startYear = startDate.getYear,
      endYear = endDate.getYear,
      hmrcSubsidyTotal = SubsidyAmount(s.hmrcSubsidyTotalEUR.setScale(2)),
      nonHmrcSubsidyTotal = SubsidyAmount(s.nonHMRCSubsidyTotalEUR.setScale(2)),
      sector = u.industrySector,
      sectorCap = u.industrySectorLimit.orElse(DefaultSectorLimits.get(u.industrySector)).map(l => IndustrySectorLimit(l.setScale(2))).get,
      allowanceRemaining = SubsidyAmount(BigDecimal(200000.00).setScale(2) - s.hmrcSubsidyTotalEUR - s.nonHMRCSubsidyTotalEUR)
    )

    def summariseByTaxYear(m: Seq[(LocalDate, SubsidyAmount)]): Map[LocalDate, SubsidyAmount] =
      m.groupBy(kv => kv._1)
        // TODO - tidy
        .map { kv =>
          kv._1 -> kv._2.map(_._2)
            .fold(SubsidyAmount.Zero)((a, b) => SubsidyAmount(a + b))
        }

    val hmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = summariseByTaxYear(
      s.hmrcSubsidyUsage
        // We assume that acceptanceDate is the correct field to use since it's mandatory. There is also an issueDate
        // field but since this is optional it makes grouping by tax year impossible if no value is present.
        // We also assume that the amount field holds a value in EUR.
        .map(i => i.acceptanceDate.toTaxYearStart -> i.amount.getOrElse(SubsidyAmount.Zero))
    )

    val nonHmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = summariseByTaxYear(
      s.nonHMRCSubsidyUsage
      .map(i => i.allocationDate.toTaxYearStart -> i.nonHMRCSubsidyAmtEUR)
    )

    // Generate summaries for each starting tax year value.
    val taxYears = (startDate.getYear to endDate.toTaxYearStart.getYear)
      .map(_ - startDate.getYear)
      .map(d => LocalDate.from(startDate).plusYears(d))
      .map(d => TaxYearSummary(
        year = d.getYear,
        hmrcSubsidyTotal = hmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero),
        nonHmrcSubsidyTotal = nonHmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero),
      ))

    FinancialDashboardSummary(
      overall = overallSummary,
      taxYears = taxYears
    )

  }
}