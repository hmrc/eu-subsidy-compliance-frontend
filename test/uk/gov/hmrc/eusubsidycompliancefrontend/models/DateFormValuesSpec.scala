package uk.gov.hmrc.eusubsidycompliancefrontend.models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues.dateValueMapping

class DateFormValuesSpec extends AnyWordSpecLike with Matchers {

  "DateFormValues" when {

    "isValidDate is called" must {

      "return true for valid date values" in {
        DateFormValues("1", "2", "2022").isValidDate mustBe true
      }

      "return false for invalid date values" in {
        DateFormValues("99", "99", "10").isValidDate mustBe false
      }

    }

  }

  "form data is validated" must {

    "return empty fields error if all date fields are empty" in {
      validateAndCheckError("", "", "")("error.date.emptyfields")
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
      validateAndCheckError("1", "1", "9999")("error.date.in-future")
    }

  }

  private def validateAndCheckError(d: String, m: String, y: String)(errorMessage: String) = {
    val result = dateValueMapping.bind(Map(
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
