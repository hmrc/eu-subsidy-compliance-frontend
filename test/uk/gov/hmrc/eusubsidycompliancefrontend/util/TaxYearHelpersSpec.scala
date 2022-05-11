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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.LocalDate

class TaxYearHelpersSpec extends AnyWordSpecLike with Matchers {

  private val BeforeTaxYearEnd  = LocalDate.parse("2024-03-01")
  private val LastDayOfTaxYear  = LocalDate.parse("2024-04-05")
  private val FirstDayOfTaxYear = LocalDate.parse("2024-04-06")
  private val AfterTaxYearEnd   = LocalDate.parse("2024-05-01")

  "taxYearStartForDate" must {

    "return a tax year starting in the previous year if the date falls before the end of the tax year" in {
      TaxYearHelpers.taxYearStartForDate(BeforeTaxYearEnd) mustBe LocalDate.parse("2023-04-06")
    }

    "return a tax year starting in the previous year if the date falls on last day of the old tax year" in {
      TaxYearHelpers.taxYearStartForDate(LastDayOfTaxYear) mustBe LocalDate.parse("2023-04-06")
    }

    "return a tax year starting in the same year if the date falls on first day of the new tax year" in {
      TaxYearHelpers.taxYearStartForDate(FirstDayOfTaxYear) mustBe LocalDate.parse("2024-04-06")
    }

    "return a tax year starting in the same year if the date falls after the start of the new tax year" in {
      TaxYearHelpers.taxYearStartForDate(AfterTaxYearEnd) mustBe LocalDate.parse("2024-04-06")
    }

  }

  "taxYearEndForDate" must {

    "return a tax year ending in the current year if the date falls before the end of the old tax year" in {
      TaxYearHelpers.taxYearEndForDate(BeforeTaxYearEnd) mustBe LocalDate.parse("2024-04-05")
    }

    "return a tax year ending in the current year if the date falls on the last day of the old tax year" in {
      TaxYearHelpers.taxYearEndForDate(LastDayOfTaxYear) mustBe LocalDate.parse("2024-04-05")
    }

    "return a tax year ending in the following year if the date falls on the first day of the new tax year" in {
      TaxYearHelpers.taxYearEndForDate(FirstDayOfTaxYear) mustBe LocalDate.parse("2025-04-05")
    }

    "return a tax year ending in the following year if the date falls after the first day of the new tax year" in {
      TaxYearHelpers.taxYearEndForDate(AfterTaxYearEnd) mustBe LocalDate.parse("2025-04-05")
    }

  }

  "earliestAllowedDate" must {

    "return the start of the earliest allowed date if the date falls before the end of the old tax year" in {
      TaxYearHelpers.earliestAllowedDate(BeforeTaxYearEnd) mustBe LocalDate.parse("2021-04-06")
    }

    "return the start of the earliest allowed tax year if the date falls on the last day of the old tax year" in {
      TaxYearHelpers.earliestAllowedDate(LastDayOfTaxYear) mustBe LocalDate.parse("2021-04-06")
    }

    "return the start of the earliest allowed tax year if the date falls on the first day of the new tax year" in {
      TaxYearHelpers.earliestAllowedDate(FirstDayOfTaxYear) mustBe LocalDate.parse("2022-04-06")
    }

    "return the start of the earliest allowed tax year if the date falls after the first day of the new tax year" in {
      TaxYearHelpers.earliestAllowedDate(AfterTaxYearEnd) mustBe LocalDate.parse("2022-04-06")
    }

    "return the earliest supported date of 2021-01-01 if the tax year start falls before this" in {
      TaxYearHelpers.earliestAllowedDate(LocalDate.parse("2021-01-01")) mustBe LocalDate.parse("2021-01-01")
    }

  }

  "searchRange" must {

    "return a valid 3 tax year search range for the specified date" in {
      TaxYearHelpers.searchRange(BeforeTaxYearEnd) mustBe((LocalDate.parse("2021-04-06"), BeforeTaxYearEnd))
    }

    "respect the earliest allowed start date if the earliest tax year start falls before it" in {
      val date = LocalDate.parse("2022-02-03")
      TaxYearHelpers.searchRange(date) mustBe((LocalDate.parse("2021-01-01"), date))
    }

  }

}
