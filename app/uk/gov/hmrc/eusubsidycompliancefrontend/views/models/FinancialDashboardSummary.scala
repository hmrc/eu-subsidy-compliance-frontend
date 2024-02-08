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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.models

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Undertaking, UndertakingBalance, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._

import java.time.{LocalDate, Month}

case class FinancialDashboardSummary(
  overall: OverallSummary,
  taxYears: Seq[TaxYearSummary] = Seq.empty,
  undertakingBalanceEUR: SubsidyAmount,
  usingAllowanceRemaining: Boolean
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
  def allowanceExceeded: Boolean = total > sectorCap
  // If the allowance has been exceeded we must show the remaining amount as zero rather than a negative number.
  def allowanceRemaining: SubsidyAmount =
    if (allowanceExceeded) SubsidyAmount(0)
    else SubsidyAmount(sectorCap - total)
}

case class TaxYearSummary(
  startYear: Int,
  hmrcSubsidyTotal: SubsidyAmount,
  nonHmrcSubsidyTotal: SubsidyAmount,
  isCurrentTaxYear: Boolean
) {
  def total: SubsidyAmount = SubsidyAmount(hmrcSubsidyTotal + nonHmrcSubsidyTotal)
  def endYear: Int = startYear + 1
}

object FinancialDashboardSummary {

  // Generates summarised data to populate the financial dashboard page.
  // All currency amounts in EUR.
  def fromUndertakingSubsidies(
    undertaking: Undertaking,
    subsidies: UndertakingSubsidies,
    balance: Option[UndertakingBalance],
    today: LocalDate
  ): FinancialDashboardSummary = {

    val startDate = today.toEarliestTaxYearStart
    val endDate = today.toTaxYearEnd

    val sectorCap: IndustrySectorLimit = undertaking.industrySectorLimit

    val overallSummary = OverallSummary(
      startYear = startDate.getYear,
      endYear = endDate.getYear,
      hmrcSubsidyTotal = SubsidyAmount(subsidies.hmrcSubsidyTotalEUR),
      nonHmrcSubsidyTotal = SubsidyAmount(subsidies.nonHMRCSubsidyTotalEUR),
      sector = undertaking.industrySector,
      sectorCap = sectorCap
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
        .map(i => i.acceptanceDate.toTaxYearStart -> i.hmrcSubsidyAmtEUR.getOrElse(SubsidyAmount.Zero))
    )

    val nonHmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = sumByTaxYear(
      subsidies.nonHMRCSubsidyUsage
        .filterNot(_.isRemoved)
        .map(i => i.allocationDate.toTaxYearStart -> i.nonHMRCSubsidyAmtEUR)
    )

    def isCurrentTaxYear(startYear: Int) = {
      val startDate = LocalDate.of(startYear, Month.APRIL, 6)
      val endDate = LocalDate.of(startYear + 1, Month.APRIL, 5)

      today.isEqual(startDate) ||
      today.isEqual(endDate) ||
      (today.isAfter(startDate) && today.isBefore(endDate))
    }

    // Generate summaries for each starting tax year value in descending year order.
    val taxYearSummaries = (endDate.toTaxYearStart.getYear to startDate.getYear by -1)
      .map(_ - startDate.getYear)
      .map(d => LocalDate.from(startDate).plusYears(d))
      .map(d =>
        TaxYearSummary(
          startYear = d.getYear,
          hmrcSubsidyTotal = hmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero),
          nonHmrcSubsidyTotal = nonHmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount.Zero),
          isCurrentTaxYear = isCurrentTaxYear(d.getYear)
        )
      )

    FinancialDashboardSummary(
      overall = overallSummary,
      taxYears = taxYearSummaries,
      undertakingBalanceEUR = balance.fold(overallSummary.allowanceRemaining)(_.availableBalanceEUR),
      usingAllowanceRemaining = balance.isEmpty
    )
  }
}
