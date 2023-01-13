/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.LocalDate

class ReportReminderHelpersSpec extends AnyWordSpecLike with Matchers {

  "ReportDeMinimisReminderHelperSpec" when {

    "isTimeToReport" must {

      def test(currentDate: LocalDate, response: Boolean) = {
        val lastSubsidyUsageUpdt = LocalDate.of(2021, 12, 1).some
        ReportReminderHelpers.isTimeToReport(lastSubsidyUsageUpdt, currentDate) mustBe response
      }

      "return true" when {

        "today's date falls between the 76th and te 90th day since last de minimis reported" in {
          test(currentDate = LocalDate.of(2022, 2, 16), response = true)
        }

        "today's date is exactly 76th day since last de minimis reported" in {
          test(currentDate = LocalDate.of(2022, 2, 15), response = true)
        }

        "today's date is exactly 90th day since last de minimis reported" in {
          test(currentDate = LocalDate.of(2022, 3, 1), response = true)
        }
      }

      "return false" when {

        "today's date falls before 76th day since last de minimis reported" in {
          test(currentDate = LocalDate.of(2022, 2, 1), response = false)
        }

        "today's date falls after 90th day since last de minimis reported" in {
          test(currentDate = LocalDate.of(2022, 3, 2), response = false)
        }

      }

    }

    "isOverdue" must {
      "return false when an empty date option is passed" in {
        ReportReminderHelpers.isOverdue(None, LocalDate.now()) mustBe false
      }
    }
  }

}
