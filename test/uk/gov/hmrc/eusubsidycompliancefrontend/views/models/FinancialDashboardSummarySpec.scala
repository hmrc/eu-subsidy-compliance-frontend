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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.{agriculture, aquaculture, other, transport}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, Sector, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{hmrcSubsidy, nonHmrcSubsidy, undertaking, undertakingSubsidies}

import java.time.LocalDate

class FinancialDashboardSummarySpec extends AnyWordSpecLike with Matchers {

  "FinancialDashboardSummary" should {

    "convert and return an instance with zero summaries where no subsidy data is present" in {
      val end   = LocalDate.parse("2022-03-01").toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart

      val emptyUndertakingSubsidies = undertakingSubsidies.copy(
        nonHMRCSubsidyTotalEUR = SubsidyAmount.Zero,
        hmrcSubsidyTotalEUR = SubsidyAmount.Zero,
        nonHMRCSubsidyUsage = List.empty,
        hmrcSubsidyUsage = List.empty
      )

      val result =
        FinancialDashboardSummary.fromUndertakingSubsidies(undertaking, emptyUndertakingSubsidies, start, end)

      val expected = FinancialDashboardSummary(
        overall = OverallSummary(
          startYear = start.getYear,
          endYear = end.getYear,
          hmrcSubsidyTotal = emptyUndertakingSubsidies.hmrcSubsidyTotalEUR,
          nonHmrcSubsidyTotal = emptyUndertakingSubsidies.nonHMRCSubsidyTotalEUR,
          sector = Sector.transport,
          sectorCap = IndustrySectorLimit(BigDecimal(12.34))
        ),
        taxYears = Seq(2021, 2020, 2019).map { year =>
          TaxYearSummary(
            startYear = year,
            hmrcSubsidyTotal = SubsidyAmount.Zero,
            nonHmrcSubsidyTotal = SubsidyAmount.Zero
          )
        }
      )

      result shouldBe expected
    }

    "convert and return a valid FinancialDashboardSummary instance" in {
      val end         = LocalDate.parse("2022-03-01").toTaxYearEnd
      val start       = end.minusYears(2).toTaxYearStart
      val yearOffsets = List(2, 1, 0)

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
          sector = Sector.transport,
          sectorCap = IndustrySectorLimit(BigDecimal(12.34))
        ),
        taxYears = Seq(
          TaxYearSummary(2021, SubsidyAmount(123.45), SubsidyAmount(123.45)),
          TaxYearSummary(2020, SubsidyAmount(123.45), SubsidyAmount(123.45)),
          TaxYearSummary(2019, SubsidyAmount(123.45), SubsidyAmount(123.45))
        )
      )

      result shouldBe expected
    }

    "apply default sector limits where none defined on the undertaking" in {
      val end   = LocalDate.parse("2022-03-01").toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart

      val emptyUndertakingSubsidies = undertakingSubsidies.copy(
        nonHMRCSubsidyTotalEUR = SubsidyAmount.Zero,
        hmrcSubsidyTotalEUR = SubsidyAmount.Zero,
        nonHMRCSubsidyUsage = List.empty,
        hmrcSubsidyUsage = List.empty
      )

      val sectorLimits = Map(
        agriculture -> IndustrySectorLimit(30000.00),
        aquaculture -> IndustrySectorLimit(20000.00),
        other       -> IndustrySectorLimit(200000.00),
        transport   -> IndustrySectorLimit(100000.00)
      )

      sectorLimits.keys.foreach { sector =>
        val undertakingForSector = undertaking.copy(
          industrySector = sector,
          industrySectorLimit = None
        )
        val result = FinancialDashboardSummary.fromUndertakingSubsidies(
          undertakingForSector,
          emptyUndertakingSubsidies,
          start,
          end
        )
        result.overall.sectorCap shouldBe sectorLimits(sector)
      }

    }

    "tax year summary should compute total correctly" in {
      TaxYearSummary(2000, SubsidyAmount(1.00), SubsidyAmount(2.00)).total shouldBe SubsidyAmount(3.00)
    }

    "overall tax summary should compute total and allowance remaining correctly" in {
      val underTest = OverallSummary(
        startYear = 2000,
        endYear = 2001,
        hmrcSubsidyTotal = SubsidyAmount(1.00),
        nonHmrcSubsidyTotal = SubsidyAmount(2.00),
        sector = Sector.other,
        sectorCap = IndustrySectorLimit(200000.00)
      )

      underTest.total shouldBe SubsidyAmount(3.00)
      underTest.allowanceRemaining shouldBe SubsidyAmount(199997.00)
    }
  }

}
