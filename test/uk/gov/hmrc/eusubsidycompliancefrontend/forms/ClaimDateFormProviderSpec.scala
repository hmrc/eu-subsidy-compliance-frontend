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
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.{LocalDate, ZoneId}

// TODO - can we pass data to the form method rather than invoking the mapping?
class ClaimDateFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val day = 1
  private val month = 1
  private val year = 2022

  private val fakeTimeProvider: TimeProvider = new TimeProvider {
    override def today: LocalDate = LocalDate.of(year, month, day)
    override def today(z: ZoneId): LocalDate = today
  }

  private val underTest = new ClaimDateFormProvider(fakeTimeProvider)

  "form data validation" must {

    "return empty fields error if all date fields are empty" in {
      validateAndCheckError("", "", "")("error.date.emptyfields")
    }

    "return empty fields error if all date fields just contain whitespace" in {
      validateAndCheckError(" ", " ", " ")("error.date.emptyfields")
    }

    "return invalid entry error if non-numeric values are entered" in {
      validateAndCheckError("foo", "bar", "baz")("error.date.invalidentry")
    }

    "return day missing error if day value not present" in {
      validateAndCheckError("", "1", "2")("error.day.missing")
    }

    "return month missing error if month value not present" in {
      validateAndCheckError("1", "", "2")("error.month.missing")
    }

    "return year missing error if year value not present" in {
      validateAndCheckError("1", "2", "")("error.year.missing")
    }

    "return day and month missing error if only year value present" in {
      validateAndCheckError("", "", "2")("error.day-and-month.missing")
    }

    "return month and year missing error if only day value present" in {
      validateAndCheckError("1", "", "")("error.month-and-year.missing")
    }

    "return day and year missing error if only month value present" in {
      validateAndCheckError("", "1", "")("error.day-and-year.missing")
    }

    "return date invalid if values do not form a valid date" in {
      validateAndCheckError("50", "20", "2000")("error.date.invalid")
    }

    "return date in future error if date is in the future" in {
      validateAndCheckError((day+1).toString, "1", "9999")("error.date.in-future")
    }

    "return date outside of tax year range error for date before the start of the tax year range" in {
      validateAndCheckError("1", "1", "1900")("error.date.outside-allowed-tax-year-range")
    }

    "return no errors for todays date" in {
      validateAndCheckSuccess(day.toString, month.toString, year.toString)
    }

    "return no errors for a date in the past that is within the tax year range" in {
      validateAndCheckSuccess(day.toString, month.toString, (year-1).toString)
    }

    "return no errors for a date in the past that is equal to the start of the allowed tax year range" in {
      validateAndCheckSuccess("6", "4", (year-3).toString)
    }

  }

  private def validateAndCheckSuccess(d: String, m: String, y: String) = {
    val result: Either[Seq[FormError], DateFormValues] = underTest.form.mapping.bind(Map(
      "day"   -> d,
      "month" -> m,
      "year"  -> y,
    ))
    result mustBe Right(DateFormValues(d, m, y))
  }

  private def validateAndCheckError(d: String, m: String, y: String)(errorMessage: String) = {
    val result = underTest.form.mapping.bind(Map(
      "day"   -> d,
      "month" -> m,
      "year"  -> y,
    ))

    // TODO - does the frontend show multiple errors or can we just fail on the first?
    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError("", errorMessage))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true
  }

}
