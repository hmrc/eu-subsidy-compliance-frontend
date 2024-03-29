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

import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, Sector, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{hmrcSubsidy, nonHmrcSubsidy, subsidyAmount, undertaking, undertakingBalance, undertakingSubsidies}

import java.time.LocalDate

class FinancialDashboardSummarySpec extends BaseSpec with Matchers {

  "FinancialDashboardSummary" should {

    "convert and return an instance with zero summaries where no subsidy data is present" in {
      val today = LocalDate.parse("2022-03-01").toTaxYearEnd
      val end = today.toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart

      val emptyUndertakingSubsidies = undertakingSubsidies.copy(
        nonHMRCSubsidyTotalEUR = SubsidyAmount.Zero,
        hmrcSubsidyTotalEUR = SubsidyAmount.Zero,
        nonHMRCSubsidyUsage = List.empty,
        hmrcSubsidyUsage = List.empty
      )

      val result =
        FinancialDashboardSummary.fromUndertakingSubsidies(
          undertaking,
          emptyUndertakingSubsidies,
          Some(undertakingBalance.copy(availableBalanceEUR = SubsidyAmount.Zero)),
          today
        )

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
            nonHmrcSubsidyTotal = SubsidyAmount.Zero,
            isCurrentTaxYear = year == 2021
          )
        },
        undertakingBalanceEUR = SubsidyAmount.Zero,
        scp08IssuesExist = false
      )

      result shouldBe expected
    }

    "convert and return a valid FinancialDashboardSummary instance" in {
      val today = LocalDate.of(2022, 3, 1)
      val end = today.toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart
      val yearOffsets = List(2, 1, 0)

      val result = FinancialDashboardSummary.fromUndertakingSubsidies(
        undertaking,
        undertakingSubsidies.copy(
          hmrcSubsidyUsage = yearOffsets.map(y => hmrcSubsidy.copy(acceptanceDate = start.plusYears(y))),
          nonHMRCSubsidyUsage = yearOffsets.map(y => nonHmrcSubsidy.copy(allocationDate = start.plusYears(y)))
        ),
        Some(undertakingBalance),
        today
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
          TaxYearSummary(2021, SubsidyAmount(123.45), SubsidyAmount(543.21), isCurrentTaxYear = true),
          TaxYearSummary(2020, SubsidyAmount(123.45), SubsidyAmount(543.21), isCurrentTaxYear = false),
          TaxYearSummary(2019, SubsidyAmount(123.45), SubsidyAmount(543.21), isCurrentTaxYear = false)
        ),
        undertakingBalanceEUR = undertakingSubsidies.hmrcSubsidyTotalEUR,
        scp08IssuesExist = false
      )

      result shouldBe expected
    }

    "ignore removed non HMRC subsidy payments" in {
      val today = LocalDate.of(2022, 3, 1)
      val end = today.toTaxYearEnd
      val start = end.minusYears(2).toTaxYearStart
      val yearOffsets = List(2, 1, 0)

      val result = FinancialDashboardSummary.fromUndertakingSubsidies(
        undertaking,
        undertakingSubsidies.copy(
          hmrcSubsidyUsage = yearOffsets.map(y => hmrcSubsidy.copy(acceptanceDate = start.plusYears(y))),
          nonHMRCSubsidyUsage = yearOffsets.map { y =>
            nonHmrcSubsidy.copy(
              allocationDate = start.plusYears(y),
              removed = Some(true)
            )
          },
          nonHMRCSubsidyTotalEUR = SubsidyAmount.Zero
        ),
        Some(undertakingBalance),
        today
      )

      val expected = FinancialDashboardSummary(
        overall = OverallSummary(
          startYear = start.getYear,
          endYear = end.getYear,
          hmrcSubsidyTotal = undertakingSubsidies.hmrcSubsidyTotalEUR,
          nonHmrcSubsidyTotal = SubsidyAmount.Zero,
          sector = Sector.transport,
          sectorCap = IndustrySectorLimit(BigDecimal(12.34))
        ),
        taxYears = Seq(
          TaxYearSummary(2021, SubsidyAmount(123.45), SubsidyAmount(0), isCurrentTaxYear = true),
          TaxYearSummary(2020, SubsidyAmount(123.45), SubsidyAmount(0), isCurrentTaxYear = false),
          TaxYearSummary(2019, SubsidyAmount(123.45), SubsidyAmount(0), isCurrentTaxYear = false)
        ),
        undertakingBalanceEUR = subsidyAmount,
        scp08IssuesExist = false
      )

      result shouldBe expected

    }

    "tax year summary should compute total correctly" in {
      TaxYearSummary(
        startYear = 2000,
        SubsidyAmount(1.00),
        SubsidyAmount(2.00),
        isCurrentTaxYear = false
      ).total shouldBe SubsidyAmount(3.00)
    }

    "overall tax summary should compute total, allowance remaining and allowance exceeded correctly" in {
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
      underTest.allowanceExceeded shouldBe false
    }
  }

  "remaining allowance should be zero if allowance has been exceeded" in {
    val underTest = OverallSummary(
      startYear = 2000,
      endYear = 2001,
      hmrcSubsidyTotal = SubsidyAmount(200000.00),
      nonHmrcSubsidyTotal = SubsidyAmount(200000.00),
      sector = Sector.other,
      sectorCap = IndustrySectorLimit(200000.00)
    )

    underTest.allowanceExceeded shouldBe true
    underTest.allowanceRemaining shouldBe SubsidyAmount(0)
  }

}
