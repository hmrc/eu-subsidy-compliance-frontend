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

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.json.digital.dateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import ClaimDateFormProvider.Errors._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors._

import java.time.LocalDate

class ClaimDateFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val day = 1
  private val month = 1
  private val year = 2022

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(day, month, year)

  private val underTest = ClaimDateFormProvider(fakeTimeProvider)

  "claim date form validation" must {

    "return empty fields error if all date fields are empty" in {
      validateAndCheckError("", "", "")(Required)
    }

    "return empty fields error if all date fields just contain whitespace" in {
      validateAndCheckError(" ", " ", " ")(Required)
    }

    "return invalid entry error if non-numeric values are entered" in {
      validateAndCheckError("foo", "bar", "baz")(IncorrectFormat)
    }

    "return day missing error if day value not present" in {
      validateAndCheckError("", "1", "2")(DayMissing)
    }

    "return month missing error if month value not present" in {
      validateAndCheckError("1", "", "2")(MonthMissing)
    }

    "return year missing error if year value not present" in {
      validateAndCheckError("1", "2", "")(YearMissing)
    }

    "return day and month missing error if only year value present" in {
      validateAndCheckError("", "", "2")(DayAndMonthMissing)
    }

    "return month and year missing error if only day value present" in {
      validateAndCheckError("1", "", "")(MonthAndYearMissing)
    }

    "return day and year missing error if only month value present" in {
      validateAndCheckError("", "1", "")(DayAndYearMissing)
    }

    "return date invalid if values do not form a valid date" in {
      validateAndCheckError("50", "20", "2000")(IncorrectFormat)
    }

    "return date in future error if date is in the future" in {
      validateAndCheckError((day + 1).toString, "1", "9999")(InFuture, "6 4 2019", "5 4 2021")
    }

    "return date outside of tax year range error for date before the start of the tax year range" in {
      validateAndCheckError("1", "1", "1900")(OutsideAllowedTaxYearRange, "6 4 2019")
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

    "return no errors for a date with 0 as prefix in month" in {
      validateAndCheckSuccess("6", "04", (year - 3).toString)
    }

  }

  private def validateAndCheckSuccess(d: String, m: String, y: String) = {
    val result: Either[Seq[FormError], DateFormValues] = underTest.form.mapping.bind(
      Map(
        "day" -> d,
        "month" -> m,
        "year" -> y
      )
    )
    val date = LocalDate.parse(LocalDate.of(y.toInt, m.toInt, d.toInt).format(dateFormatter), dateFormatter)
    val dateFormValues = DateFormValues(date.getDayOfMonth.toString, date.getMonthValue.toString, y)
    result mustBe Right(dateFormValues)
  }

  private def validateAndCheckError(d: String, m: String, y: String)(errorMessage: String, args: String*) = {
    val result = underTest.form.mapping.bind(
      Map(
        "day" -> d,
        "month" -> m,
        "year" -> y
      )
    )

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError("", s"add-claim-date.$errorMessage", args))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"could not locate error message ending '$errorMessage' in list of errors: ${result.leftSideValue}"
  }

}
