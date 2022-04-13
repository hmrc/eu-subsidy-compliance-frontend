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

package uk.gov.hmrc.eusubsidycompliancefrontend.syntax

import uk.gov.hmrc.eusubsidycompliancefrontend.util.TaxYearHelpers

import java.time.LocalDate

object TaxYearSyntax {

  implicit class LocalDateTaxYearOps(val d: LocalDate) extends AnyVal {
    def toTaxYearStart: LocalDate = TaxYearHelpers.taxYearStartForDate(d)
    def toTaxYearEnd: LocalDate = TaxYearHelpers.taxYearEndForDate(d)
    def toEarliestTaxYearStart: LocalDate = TaxYearHelpers.earliestAllowedDate(d)
    def toSearchRange: (LocalDate, LocalDate) = TaxYearHelpers.searchRange(d)
  }

}
