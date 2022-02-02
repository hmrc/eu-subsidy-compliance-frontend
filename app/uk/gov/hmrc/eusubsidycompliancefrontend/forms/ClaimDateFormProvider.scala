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
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TaxYearHelpers.taxYearStartForDate
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.util.Try

class ClaimDateFormProvider @Inject()(timeProvider: TimeProvider) extends FormProvider[DateFormValues]{

  override val mapping: Mapping[DateFormValues] = tuple(
    "day"   -> text,
    "month" -> text,
    "year"  -> text
  )
  .transform(
    { case (d, m, y) => (d.trim, m.trim, y.trim) },
    { v: (String, String, String) => v }
  )
  .verifying(
    "error.date.emptyfields",
    _ match {
      case ("", "", "") => false
      case _ => true
    })
  .verifying(
    "error.date.invalidentry",
    _ match {
      case (d, m, y) => Try(s"$d$m$y".toInt).isSuccess
      case _ => false
    })
  .verifying(
    "error.day.missing",
    _ match {
      case ("", _, _) => false
      case _ => true
    })
  .verifying(
    "error.month.missing",
    _ match {
      case (_, "", _) => false
      case _ => true
    })
  .verifying(
    "error.year.missing",
    _ match {
      case (_, _, "") => false
      case _ => true
    })
  .verifying(
    "error.day-and-month.missing",
    _ match {
      case ("", "", _) => false
      case _ => true
    })
  .verifying(
    "error.month-and-year.missing",
    _ match {
      case (_, "", "") => false
      case _ => true
   })
  .verifying(
    "error.day-and-year.missing",
    _ match {
      case ("", _, "") => false
      case _ => true
    })
  .verifying(
    "error.date.invalid",
    _ match {
      case (d: String, m: String, y: String)  => localDateFromValues(d, m, y).isSuccess
      case _ => true
    })
  .verifying(
    "error.date.in-future",
    _ match {
      case (d: String, m: String, y: String) => localDateFromValues(d, m, y).map { d =>
        val today = timeProvider.today(ZoneId.of("Europe/London"))
        !d.isAfter(today)
      }.getOrElse(true)
      case _ => true
  })
  .verifying(
    "error.date.outside-allowed-tax-year-range",
    _ match {
      case (d: String, m: String, y: String) => localDateFromValues(d, m, y).map { d =>
        val today = timeProvider.today(ZoneId.of("Europe/London"))
        // We allow claims for the current or previous 2 tax years.
        val earliestAllowedDate = taxYearStartForDate(today).minusYears(2)
        !d.isBefore(earliestAllowedDate)
      }.getOrElse(true)
      case _ => false
    }
  )
  .transform(
    { case (d, m, y) => DateFormValues(d,m,y) },
    d => (d.day, d.month, d.year)
  )

  private def localDateFromValues(d: String, m: String, y: String) = Try(LocalDate.of(y.toInt, m.toInt, d.toInt))

  override val form: Form[DateFormValues] = Form(mapping)

}