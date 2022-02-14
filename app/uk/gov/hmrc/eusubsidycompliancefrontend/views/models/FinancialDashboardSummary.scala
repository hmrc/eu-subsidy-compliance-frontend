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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyAmount
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
  // TODO - check these types
  sectorCap: SubsidyAmount,
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

  // TODO - we can take an optional value from the undertaking. If this isn't present we
  //        should fallback to a default and warn.
  //        the default should reflect the appropriate cap for each sector
  val SectorCap = BigDecimal(200000.00).setScale(2)

  // TODO - we could just have the start of the first tax year and compute the rest.
  def fromUndertakingSubsidies(
    u: Undertaking,
    s: UndertakingSubsidies,
    startDate: LocalDate,
    // TODO - this should be the search end date, not the tax year end date
    endDate: LocalDate
  ): FinancialDashboardSummary = {

    val overallSummary = OverallSummary(
      startYear = startDate.getYear,
      endYear = endDate.getYear,
      hmrcSubsidyTotal = SubsidyAmount(s.hmrcSubsidyTotalEUR.setScale(2)),
      nonHmrcSubsidyTotal = SubsidyAmount(s.nonHMRCSubsidyTotalEUR.setScale(2)),
      sector = u.industrySector,
      sectorCap = SubsidyAmount(u.industrySectorLimit.map(_.setScale(2)).getOrElse(SectorCap)),
      allowanceRemaining = SubsidyAmount(BigDecimal(200000.00).setScale(2) - s.hmrcSubsidyTotalEUR - s.nonHMRCSubsidyTotalEUR)
    )

    /**
     * Simplest thing to do here is link each subsidy total to a tax year and then group by that in order to produce
     * a number of sublists that can then be summed up.
     *
     * TODO - each summary has dates - which ones do we group by?
     */

    // TODO - confirm which date refers to the tax year to use
    // TODO - confirm that the amount is in euros?
    // TODO - extract common logic for hmrc and non-hmrc subs
    val hmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = s.hmrcSubsidyUsage
      .filter(_.issueDate.isDefined) // TODO - can this ever be empty?
      .map(i => i.issueDate.get.toTaxYearStart -> i.amount.getOrElse(SubsidyAmount(BigDecimal(0.00).setScale(2))))
      .groupBy(kv => kv._1)
      // TODO - tidy
      .map { kv =>
        kv._1 -> kv._2.map(_._2)
          .fold(SubsidyAmount(BigDecimal(0.00).setScale(2)))((a: SubsidyAmount, b: SubsidyAmount) => SubsidyAmount(a + b))
      }

    // TODO - confirm that allocation date is the date to use
    val nonHmrcSubsidiesByTaxYearStart: Map[LocalDate, SubsidyAmount] = s.nonHMRCSubsidyUsage
      .map(i => i.allocationDate.toTaxYearStart -> i.nonHMRCSubsidyAmtEUR)
      .groupBy(kv => kv._1)
      // TODO - tidy
      .map { kv =>
        kv._1 -> kv._2.map(_._2)
          .fold(SubsidyAmount(BigDecimal(0.00).setScale(2)))((a: SubsidyAmount, b: SubsidyAmount) => SubsidyAmount(a + b))
      }

    // Determine number of years to compute.
    val lastTaxYearStart = endDate.toTaxYearStart

    val taxYears = (startDate.getYear to lastTaxYearStart.getYear)
      .map(_ - startDate.getYear)
      .map(d => LocalDate.from(startDate).plusYears(d))
      .map(d => TaxYearSummary(
        year = d.getYear,
        hmrcSubsidyTotal = hmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount(BigDecimal(0.00).setScale(2))),
        nonHmrcSubsidyTotal = nonHmrcSubsidiesByTaxYearStart.getOrElse(d, SubsidyAmount(BigDecimal(0.00).setScale(2))),
      ))

    FinancialDashboardSummary(
      overall = overallSummary,
      taxYears = taxYears
    )

  }
}