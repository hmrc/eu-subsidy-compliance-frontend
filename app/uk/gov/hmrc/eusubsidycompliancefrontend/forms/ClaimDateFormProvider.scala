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
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.DateFormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.util.Try

import java.time.format.DateTimeFormatter

class ClaimDateFormProvider @Inject()(timeProvider: TimeProvider) extends FormProvider[DateFormValues] {

  override val form: Form[DateFormValues] = Form(mapping)

  override protected def mapping: Mapping[DateFormValues] = tuple(
    "day"   -> text,
    "month" -> text,
    "year"  -> text
  )
  .transform(
    { case (d, m, y) => (d.trim, m.trim, y.trim) },
    { v: (String, String, String) => v }
  )
  .verifying(
    messageKeyForError("date.emptyfields"),
    _ match {
      case ("", "", "") => false
      case _ => true
    })
  .verifying(
    messageKeyForError("date.invalidentry"),
    _ match {
      case (d, m, y) => Try(s"$d$m$y".toInt).isSuccess
      case _ => false
    })
  .verifying(
    messageKeyForError("day.missing"),
    _ match {
      case ("", _, _) => false
      case _ => true
    })
  .verifying(
    messageKeyForError("month.missing"),
    _ match {
      case (_, "", _) => false
      case _ => true
    })
  .verifying(
    messageKeyForError("year.missing"),
    _ match {
      case (_, _, "") => false
      case _ => true
    })
  .verifying(
    messageKeyForError("day-and-month.missing"),
    _ match {
      case ("", "", _) => false
      case _ => true
    })
  .verifying(
    messageKeyForError("month-and-year.missing"),
    _ match {
      case (_, "", "") => false
      case _ => true
    })
  .verifying(
    messageKeyForError("day-and-year.missing"),
    _ match {
      case ("", _, "") => false
      case _ => true
    })
  .verifying(
    messageKeyForError("date.invalid"),
    _ match {
      case (d: String, m: String, y: String) => localDateFromValues(d, m, y).isSuccess
      case _ => true
    })
  .verifying(
    messageKeyForError("date.in-future"),
    _ match {
      case (d: String, m: String, y: String) => localDateFromValues(d, m, y).map { d =>
        val today = timeProvider.today(ZoneId.of("Europe/London"))
        !d.isAfter(today)
      }.getOrElse(true)
      case _ => true
    })
  .verifying(Constraint { (d: (String, String, String)) =>
      val earliestAllowedDate = timeProvider.today(ZoneId.of("Europe/London")).toEarliestTaxYearStart
      localDateFromValues(d._1, d._2, d._3).map { parsedDate =>
        if (parsedDate.isBefore(earliestAllowedDate)) {
          Invalid(Seq(
            ValidationError(
              messageKeyForError("date.outside-allowed-tax-year-range"),
              earliestAllowedDate.format(DateTimeFormatter.ofPattern("dd MM yyyy"))
            )))
        }
        else Valid
      }.getOrElse(Valid)
  })
  .transform(
    { case (d, m, y) => DateFormValues(d,m,y) },
    d => (d.day, d.month, d.year)
  )

  private def messageKeyForError(error: String) = s"add-claim-date.error.$error"

  // TODO - this should live on DateValues maybe?
  private def localDateFromValues(d: String, m: String, y: String) = Try(LocalDate.of(y.toInt, m.toInt, d.toInt))

}