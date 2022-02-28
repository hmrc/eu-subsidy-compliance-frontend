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

import java.time.{LocalDate, Month}

object TaxYearHelpers {

  def taxYearStartForDate(d: LocalDate): LocalDate = {
    val taxYearStartForDateYear = LocalDate.of(d.getYear, Month.APRIL, 6)
    if (d.isBefore(taxYearStartForDateYear)) taxYearStartForDateYear.minusYears(1)
    else taxYearStartForDateYear
  }

  def taxYearEndForDate(d: LocalDate): LocalDate =
    taxYearStartForDate(d)
      .plusYears(1)
      .minusDays(1)

  // Since the allowed date range is the current and previous 2 tax years the earliest allowed date is then the start
  // of the earliest tax year.
  def earliestAllowedDate(d: LocalDate): LocalDate = taxYearStartForDate(d).minusYears(2)

}
