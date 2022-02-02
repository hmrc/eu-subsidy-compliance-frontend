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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import play.api.data.Forms.{text, tuple}
import play.api.data.Mapping
import play.api.libs.json.{Json, OFormat}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import scala.util.Try

case class DateFormValues(day: String, month: String, year: String) {

  // TODO -review usages - is this needed?
  def isValidDate: Boolean = {
    val dateText = s"${"%02d".format(day.toInt)}/${"%02d".format(month.toInt)}/$year"
    Try(LocalDate.parse(dateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"))).isSuccess
  }

  def toFormattedString: String = day + "/" + month + "/" + year

}

case object DateFormValues {

  implicit val format: OFormat[DateFormValues] = Json.format[DateFormValues]

  val dateValueMapping: Mapping[DateFormValues] = tuple(
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
      case (d: String, m: String, y: String)  => Try(LocalDate.of(y.toInt, m.toInt, d.toInt)).isSuccess
      case _ => true
    }
  )
  .verifying(
    "error.date.in-future",
    _ match {
      case (d: String, m: String, y: String) => Try {
        LocalDate.of(y.toInt, m.toInt, d.toInt)
          .isBefore(LocalDate.now(ZoneId.of("Europe/London")))
      // If we can't parse the date we let the validation constraint pass since we can't check it
      }.getOrElse(true)
      case _ => true
  })
  .transform(
    { case (d, m, y) => DateFormValues(d,m,y) },
    d => (d.day, d.month, d.year)
  )

}
