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

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyRef
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{fixedDate, nonHmrcSubsidy, undertakingSubsidies}

class UndertakingSubsidiesSpec extends AnyWordSpec with Matchers {

  "UndertakingSubsidies" when {

    "hasNeverSubmitted is called" must {

      "return true if no subsidy records are present" in {
        val underTest = undertakingSubsidies.copy(nonHMRCSubsidyUsage = List.empty)
        underTest.hasNeverSubmitted shouldBe true
      }

      "return false if subsidy records are present" in {
        val underTest = undertakingSubsidies
        underTest.hasNeverSubmitted shouldBe false
      }

    }

    "lastSubmitted is called" must {

      "return None if no subsidies have been recorded" in {
        val underTest = undertakingSubsidies.copy(nonHMRCSubsidyUsage = List.empty)
        underTest.lastSubmitted shouldBe None
      }

      "return the submission date if there is a single subsidy" in {
        val underTest = undertakingSubsidies
        underTest.lastSubmitted shouldBe Some(fixedDate)
      }

      "return the most recent submission date if there are multiple subsidies" in {
        val underTest = undertakingSubsidies.copy(nonHMRCSubsidyUsage = List(
          nonHmrcSubsidy.copy(submissionDate = fixedDate.minusDays(2)),
          nonHmrcSubsidy.copy(submissionDate = fixedDate.minusDays(1)),
          nonHmrcSubsidy.copy(submissionDate = fixedDate.minusDays(0)),
        ))

        underTest.lastSubmitted shouldBe Some(fixedDate)
      }

    }

    "forReportedPaymentsPage" must {

      "return non HMRC subsidy usage in reverse order of submission date" in {
        val subsidy1 = nonHmrcSubsidy.copy(submissionDate = fixedDate.plusDays(1))
        val subsidy2 = nonHmrcSubsidy.copy(submissionDate = fixedDate.plusDays(2))
        val subsidy3 = nonHmrcSubsidy.copy(submissionDate = fixedDate.plusDays(3))

        val underTest = undertakingSubsidies.copy(nonHMRCSubsidyUsage = List(
          subsidy1,
          subsidy2,
          subsidy3
        ))

        underTest.forReportedPaymentsPage.nonHMRCSubsidyUsage shouldBe List(subsidy3, subsidy2, subsidy1)
      }
    }

    "findNonHmrcSubsidy" must {

      val subsidy1 = nonHmrcSubsidy.copy(subsidyUsageTransactionId = SubsidyRef("12").some)
      val subsidy2 = nonHmrcSubsidy.copy(subsidyUsageTransactionId = SubsidyRef("34").some)
      val subsidy3 = nonHmrcSubsidy.copy(subsidyUsageTransactionId = SubsidyRef("56").some, removed = true.some)

      val underTest = undertakingSubsidies.copy(nonHMRCSubsidyUsage = List(
        subsidy1,
        subsidy2,
        subsidy3
      ))


      "return None if no subsidy matching given transaction ID is found" in {
        underTest.findNonHmrcSubsidy("This ID does not exist") shouldBe None
      }

      "return the subsidy matching the given transaction ID where one exists" in {
        underTest.findNonHmrcSubsidy("12") should contain(subsidy1)
      }

      "ignore subsidies that are marked as removed" in {
          underTest.findNonHmrcSubsidy("56") shouldBe None
      }

    }
  }
}
