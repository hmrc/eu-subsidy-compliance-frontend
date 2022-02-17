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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import java.time.LocalDate

object ReportDeMinimisReminderHelper {

  val ONE_DAY_BEFORE_REMINDER_DAY = 75
  val ONE_DAY_AFTER_DUE_DAY = 91
  val REPORT_DEMINIMIS_DUE_DAY = 90

  def isTimeToReport(lastSubsidyUsageUpdt: Option[LocalDate], currentDate: LocalDate) = {
    lastSubsidyUsageUpdt
      .map(date => (date.plusDays(ONE_DAY_BEFORE_REMINDER_DAY)
        .isBefore(currentDate) &&
        (date.plusDays(ONE_DAY_AFTER_DUE_DAY).isAfter(currentDate)))).getOrElse(false)
  }

  def dueDateToReport(lastSubsidyUsageUpdt: Option[LocalDate]) = lastSubsidyUsageUpdt.map(_.plusDays(REPORT_DEMINIMIS_DUE_DAY))


}
