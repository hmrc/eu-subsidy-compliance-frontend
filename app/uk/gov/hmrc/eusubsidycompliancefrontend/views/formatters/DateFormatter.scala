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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters

import play.api.i18n.Messages

import java.time.LocalDate

object DateFormatter {

  def govDisplayFormat(date: LocalDate)(implicit messages: Messages): String =
    constructDateString(date, "date.")

  def govDisplayFormatTruncated(date: LocalDate)(implicit messages: Messages): String =
    constructDateString(date, "date.truncated.")

  private def constructDateString(date: LocalDate, monthKeyPrefix: String)(implicit messages: Messages) = Seq(
    date.getDayOfMonth,
    messages(monthKeyPrefix + date.getMonthValue),
    date.getYear
  ).mkString(" ")

  object Syntax {
    implicit class DateOps(val d: LocalDate) extends AnyVal {
      def toDisplayFormat(implicit m: Messages): String      = govDisplayFormat(d)
      def toShortDisplayFormat(implicit m: Messages): String = govDisplayFormatTruncated(d)
    }
  }

}
