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

package uk.gov.hmrc.eusubsidycompliancefrontend.forms

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider

class ClaimDateFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val day   = 1
  private val month = 1
  private val year  = 2022

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(day, month, year)

  private val underTest = new ClaimDateFormProvider(fakeTimeProvider)

  "claim date form validation" must {

    "return empty fields error if all date fields are empty" in {
      validateAndCheckError("", "", "")("date.emptyfields")
    }

    "return empty fields error if all date fields just contain whitespace" in {
      validateAndCheckError(" ", " ", " ")("date.emptyfields")
    }

    "return invalid entry error if non-numeric values are entered" in {
      validateAndCheckError("foo", "bar", "baz")("date.invalidentry")
    }

    "return day missing error if day value not present" in {
      validateAndCheckError("", "1", "2")("day.missing")
    }

    "return month missing error if month value not present" in {
      validateAndCheckError("1", "", "2")("month.missing")
    }

    "return year missing error if year value not present" in {
      validateAndCheckError("1", "2", "")("year.missing")
    }

    "return day and month missing error if only year value present" in {
      validateAndCheckError("", "", "2")("day-and-month.missing")
    }

    "return month and year missing error if only day value present" in {
      validateAndCheckError("1", "", "")("month-and-year.missing")
    }

    "return day and year missing error if only month value present" in {
      validateAndCheckError("", "1", "")("day-and-year.missing")
    }

    "return date invalid if values do not form a valid date" in {
      validateAndCheckError("50", "20", "2000")("date.invalid")
    }

    "return date in future error if date is in the future" in {
      validateAndCheckError((day + 1).toString, "1", "9999")("date.in-future", "06 04 2019", "05 04 2021")
    }

    "return date outside of tax year range error for date before the start of the tax year range" in {
      validateAndCheckError("1", "1", "1900")("date.outside-allowed-tax-year-range", "06 04 2019")
    }

    "return no errors for todays date" in {
      validateAndCheckSuccess(day.toString, month.toString, year.toString)
    }

    "return no errors for a date in the past that is within the tax year range" in {
      validateAndCheckSuccess(day.toString, month.toString, (year - 1).toString)
    }

    "return no errors for a date in the past that is equal to the start of the allowed tax year range" in {
      validateAndCheckSuccess("6", "4", (year - 3).toString)
    }

  }

  private def validateAndCheckSuccess(d: String, m: String, y: String) = {
    val result: Either[Seq[FormError], DateFormValues] = underTest.form.mapping.bind(
      Map(
        "day"   -> d,
        "month" -> m,
        "year"  -> y
      )
    )
    result mustBe Right(DateFormValues(d, m, y))
  }

  private def validateAndCheckError(d: String, m: String, y: String)(errorMessage: String, args: String*) = {
    val result = underTest.form.mapping.bind(
      Map(
        "day"   -> d,
        "month" -> m,
        "year"  -> y
      )
    )

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError("", s"add-claim-date.error.$errorMessage", args))
      case _            => false
    }

    foundExpectedErrorMessage mustBe true
  }

}
