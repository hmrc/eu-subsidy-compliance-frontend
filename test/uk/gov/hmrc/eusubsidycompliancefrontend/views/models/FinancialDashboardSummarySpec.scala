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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, Sector, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.Fixtures.{hmrcSubsidy, nonHmrcSubsidy, undertaking, undertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TaxYearSyntax.LocalDateTaxYearOps

import java.time.LocalDate

class FinancialDashboardSummarySpec extends AnyWordSpecLike with Matchers {

  "FinancialDashboardSummary" should {

    "convert and return an instance with zero summaries where no subsidy data is present" in {
      val end = LocalDate.parse("2022-03-01").toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart

      val emptyUndertakingSubsidies = undertakingSubsidies.copy(
        nonHMRCSubsidyTotalEUR = SubsidyAmount.ZeroToTwoDecimalPlaces,
        hmrcSubsidyTotalEUR = SubsidyAmount.ZeroToTwoDecimalPlaces,
        nonHMRCSubsidyUsage = List.empty,
        hmrcSubsidyUsage = List.empty
      )

      val result = FinancialDashboardSummary.fromUndertakingSubsidies(undertaking, emptyUndertakingSubsidies, start, end)

      val expected = FinancialDashboardSummary(
        overall = OverallSummary(
          startYear = start.getYear,
          endYear = end.getYear,
          hmrcSubsidyTotal = emptyUndertakingSubsidies.hmrcSubsidyTotalEUR,
          nonHmrcSubsidyTotal = emptyUndertakingSubsidies.nonHMRCSubsidyTotalEUR,
          sector = Sector.other,
          sectorCap = IndustrySectorLimit(BigDecimal(200000.00)),
          allowanceRemaining = SubsidyAmount(BigDecimal(200000.00)),
        ),
        taxYears = Seq(2019, 2020, 2021).map { year =>
          TaxYearSummary(
            year = year,
            hmrcSubsidyTotal = SubsidyAmount.ZeroToTwoDecimalPlaces,
            nonHmrcSubsidyTotal = SubsidyAmount.ZeroToTwoDecimalPlaces
          )
        }
      )

      result shouldBe expected
    }

    "convert and return a valid FinancialDashboardSummary instance" in {
      val end = LocalDate.parse("2022-03-01").toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart
      val yearOffsets = List(0, 1, 2)

      val result = FinancialDashboardSummary.fromUndertakingSubsidies(
        undertaking,
        undertakingSubsidies.copy(
          hmrcSubsidyUsage = yearOffsets.map(y => hmrcSubsidy.copy(acceptanceDate = start.plusYears(y))),
          nonHMRCSubsidyUsage = yearOffsets.map(y => nonHmrcSubsidy.copy(allocationDate = start.plusYears(y)))
        ),
        start,
        end
      )

      val expected = FinancialDashboardSummary(
        overall = OverallSummary(
          startYear = start.getYear,
          endYear = end.getYear,
          hmrcSubsidyTotal = undertakingSubsidies.hmrcSubsidyTotalEUR,
          nonHmrcSubsidyTotal = undertakingSubsidies.nonHMRCSubsidyTotalEUR,
          sector = Sector.other,
          sectorCap = IndustrySectorLimit(BigDecimal(200000.00)),
          allowanceRemaining = SubsidyAmount(BigDecimal(200000.00) - undertakingSubsidies.hmrcSubsidyTotalEUR - undertakingSubsidies.nonHMRCSubsidyTotalEUR),
        ),
        taxYears = Seq(
          TaxYearSummary(2019,  SubsidyAmount(123.45),  SubsidyAmount(123.45)),
          TaxYearSummary(2020,  SubsidyAmount(123.45),  SubsidyAmount(123.45)),
          TaxYearSummary(2021,  SubsidyAmount(123.45),  SubsidyAmount(123.45)),
        )
      )

      result shouldBe expected
    }
  }

}
