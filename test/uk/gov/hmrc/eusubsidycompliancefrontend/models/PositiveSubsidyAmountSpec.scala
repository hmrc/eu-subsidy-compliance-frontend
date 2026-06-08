/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{MaxInputValue, PositiveSubsidyAmount}

class PositiveSubsidyAmountSpec extends AnyWordSpec with Matchers {

  "PositiveSubsidyAmount.of" should {

    "accept zero" in {
      PositiveSubsidyAmount.of(BigDecimal("0")) shouldBe defined
    }

    "accept a positive value" in {
      PositiveSubsidyAmount.of(BigDecimal("123.45")) shouldBe defined
    }

    "accept the maximum value" in {
      PositiveSubsidyAmount.of(MaxInputValue) shouldBe defined
    }

    "reject negative values" in {
      PositiveSubsidyAmount.of(BigDecimal("-0.01")) shouldBe None
    }

    "reject values greater than the maximum" in {
      PositiveSubsidyAmount.of(MaxInputValue + 1) shouldBe None
    }

    "accept values with exactly 2 decimal places" in {
      PositiveSubsidyAmount.of(BigDecimal("123.45")) shouldBe defined
    }

    "reject values with more than 2 decimal places" in {
      PositiveSubsidyAmount.of(BigDecimal("123.456")) shouldBe None
    }
  }

  "PositiveSubsidyAmount.apply" should {

    "return a tagged value for valid input" in {
      noException shouldBe thrownBy {
        PositiveSubsidyAmount(BigDecimal("100.00"))
      }
    }

    "throw IllegalArgumentException for invalid input" in {
      val exception = intercept[IllegalArgumentException] {
        PositiveSubsidyAmount(BigDecimal("-1"))
      }

      exception.getMessage should include ("not a valid PositiveSubsidyAmount")
    }
  }
}