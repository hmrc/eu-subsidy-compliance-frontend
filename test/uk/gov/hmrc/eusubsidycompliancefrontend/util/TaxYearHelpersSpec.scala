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

  "taxYearStartForDate" must {

    "return a tax year starting in the same year if the date falls on the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-04-06")) mustBe LocalDate.parse("2022-04-06")
    }

    "return a tax year starting in the same year if the date falls after the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-12-06")) mustBe LocalDate.parse("2022-04-06")
    }

    "return a tax year starting in the previous year if the date falls before the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-02-06")) mustBe LocalDate.parse("2021-04-06")
    }

  }

  "taxYearEndForDate" must {

    "return a tax year ending in the following year if the date falls on the 6th of April" in {
      TaxYearHelpers.taxYearEndForDate(LocalDate.parse("2022-04-06")) mustBe LocalDate.parse("2023-04-05")
    }

    "return a tax year ending in the following year if the date falls after the 6th of April" in {
      TaxYearHelpers.taxYearEndForDate(LocalDate.parse("2022-12-06")) mustBe LocalDate.parse("2023-04-05")
    }

    "return a tax year ending in the current year if the date falls before the 6th of April" in {
      TaxYearHelpers.taxYearEndForDate(LocalDate.parse("2022-02-06")) mustBe LocalDate.parse("2022-04-05")
    }

  }

}
