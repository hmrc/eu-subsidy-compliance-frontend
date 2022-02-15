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

import play.api.libs.json.{Json, OFormat}
import play.api.i18n.Messages

import java.time.LocalDate

case class DateFormValues(day: String, month: String, year: String) {
  def toFormattedString: String = day + "/" + month + "/" + year

  def govDisplayFormat()(implicit messages: Messages): String =
    s"""${day} ${messages(
      s"date.${month}"
    )} ${year}"""

  def toLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)
}

object DateFormValues {

  def fromDate(localDate: LocalDate): DateFormValues =
    DateFormValues(
      localDate.getDayOfMonth.toString,
      localDate.getMonthValue.toString,
      localDate.getYear.toString
    )

  implicit val format: OFormat[DateFormValues] = Json.format[DateFormValues]
}
