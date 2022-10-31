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

import play.api.data.Forms.{text, tuple}
import play.api.data.Mapping
import play.api.data.validation._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimDateFormProvider.Errors._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimDateFormProvider.Fields._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import scala.util.Try

case class ClaimDateFormProvider(timeProvider: TimeProvider) extends FormProvider[DateFormValues] {

  private type RawFormValues = (String, String, String)

  private val dateFormatter = DateTimeFormatter.ofPattern("d M yyyy")

  private def formValueMapping = tuple(
    Day   -> text,
    Month -> text,
    Year  -> text
  )

  override protected def mapping: Mapping[DateFormValues] =
    formValueMapping
      .transform({ case (d, m, y) => (d.trim, m.trim, y.trim) }, { v: RawFormValues => v })
      .verifying(Constraint(allDateValuesEntered(_)))
      .verifying(Constraint(dateIsValid(_)))
      .verifying(Constraint(dateInAllowedRange(_)))
      .transform(
        { case (d, m, y) =>
          // day month and year string first formatted like d M YYYY, to get rid of 0 prefix in day/month value entered by the User
          //The formatted string then changed to LocalDate to fetch the date and month using the getDayOfMonth,getMonthValue  functions
          val date = LocalDate.parse(LocalDate.of(y.toInt, m.toInt, d.toInt).format(dateFormatter), dateFormatter)
          DateFormValues(date.getDayOfMonth.toString, date.getMonthValue.toString, y)
        },
        d => (d.day, d.month, d.year)
      )

  private val dateIsValid: RawFormValues => ValidationResult = {
    case (d, m, y) if Try(s"$d$m$y").isSuccess && localDateFromValues(d, m, y).isSuccess => Valid
    case _ => invalid(IncorrectFormat)
  }

  private val allDateValuesEntered: RawFormValues => ValidationResult = {
    case ("", "", "") => invalid(Required)
    case ("", "", _) => invalid(DayAndMonthMissing)
    case (_, "", "") => invalid(MonthAndYearMissing)
    case ("", _, "") => invalid(DayAndYearMissing)
    case ("", _, _) => invalid(DayMissing)
    case (_, "", _) => invalid(MonthMissing)
    case (_, _, "") => invalid(YearMissing)
    case _ => Valid
  }

  private val dateInAllowedRange: RawFormValues => ValidationResult = {
    case (d, m, y) =>
      localDateFromValues(d, m, y)
        .map { parsedDate =>
          val today = timeProvider.today(ZoneId.of("Europe/London"))
          val earliestAllowedDate = today.toEarliestTaxYearStart

          if (parsedDate.isBefore(earliestAllowedDate))
            invalid(OutsideAllowedTaxYearRange, earliestAllowedDate.format(dateFormatter))
          else if (parsedDate.isAfter(today))
            invalid(
              InFuture,
              earliestAllowedDate.format(dateFormatter),
              today.toTaxYearEnd.minusYears(1).format(dateFormatter)
            )
          else Valid
        }
        .getOrElse(Valid)
    case _ => Valid
  }

  private def invalid(error: String, params: String*) =
    Invalid(
      Seq(
        ValidationError(
          messageKeyForError(error),
          params: _*
        )
      )
    )

  private def messageKeyForError(error: String) = s"add-claim-date.$error"

  private def localDateFromValues(d: String, m: String, y: String) = Try(LocalDate.of(y.toInt, m.toInt, d.toInt))

}

object ClaimDateFormProvider {

  object Fields {
    val Day   = "day"
    val Month = "month"
    val Year  = "year"
  }

  object Errors {
    val DayAndMonthMissing          = "error.day-and-month-missing"
    val DayAndYearMissing           = "error.day-and-year-missing"
    val DayMissing                  = "error.day-missing"
    val InFuture                    = "error.in-future"
    val MonthAndYearMissing         = "error.month-and-year-missing"
    val MonthMissing                = "error.month-missing"
    val OutsideAllowedTaxYearRange  = "error.outside-allowed-tax-year-range"
    val YearMissing                 = "error.year-missing"
  }

}