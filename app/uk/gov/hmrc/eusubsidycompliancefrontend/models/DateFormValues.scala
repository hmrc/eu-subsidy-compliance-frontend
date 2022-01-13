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
import play.api.libs.json.Json

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.Try

case class DateFormValues(day: String, month: String, year: String) {
  def isValidDate: Boolean = try {
    val dateText = s"${"%02d".format(day.toInt)}/${"%02d".format(month.toInt)}/$year"
    LocalDate.parse(dateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    true
  }
  catch {
    case _ => false
  }

}

case object DateFormValues {

  def trim(inputStr: String) = inputStr.trim()

  lazy val vatRegDateMapping: Mapping[DateFormValues] = tuple(
    "day"   -> text,
    "month" -> text,
    "year"  -> text
  ).verifying(
    "error.date.emptyfields",
    x =>
      x match {
        case (d: String, m: String, y: String) if trim(d) == "" && trim(m) == "" && trim(y) == "" => false
        case _                                                                                      => true
      })
    .verifying(
      "error.day.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) != "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.month.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) == "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.day-and-month.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) == "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.month-and-year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) == "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.day-and-year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) != "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.date.invalid",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) != "" => Try(LocalDate.of(trim(y).toInt, trim(m).toInt, trim(d).toInt)).isSuccess
          case _ => true
        }
    )
    .verifying(
      "error.date.in-future",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) != "" =>
            LocalDate.of(trim(y).toInt, trim(m).toInt, trim(d).toInt)
              .isBefore(LocalDate.now(ZoneId.of("Europe/London")))
          case _ => true
        }
    )
    .transform(
      { case (d, m, y) => DateFormValues(d,m,y) },
      duy => (duy.day, duy.month, duy.year)
    )

  implicit val format = Json.format[DateFormValues]
}
