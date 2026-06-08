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

  "PositiveSubsidyAmount.validate" should {

    "accept zero" in {
      PositiveSubsidyAmount.validate(BigDecimal("0")) shouldBe PositiveSubsidyAmount(BigDecimal(0))
    }

    "accept a positive value" in {
      PositiveSubsidyAmount.validate(BigDecimal("123.45")) shouldBe PositiveSubsidyAmount(BigDecimal(123.45))
    }

    "accept the maximum value" in {
      PositiveSubsidyAmount.validate(MaxInputValue) shouldBe PositiveSubsidyAmount(BigDecimal(MaxInputValue))
    }

    "reject negative values" in {
      val exception = intercept[IllegalArgumentException] {
        PositiveSubsidyAmount.validate(BigDecimal("-0.01"))
      }

      exception.getMessage shouldBe "-0.01 is not a valid PositiveSubsidyAmount"
    }

    "reject values greater than the maximum" in {
      val exception = intercept[IllegalArgumentException] {
        PositiveSubsidyAmount.validate(MaxInputValue + 1)
      }

      exception.getMessage shouldBe "100000000000.99 is not a valid PositiveSubsidyAmount"
    }

    "accept values with exactly 2 decimal places" in {
      PositiveSubsidyAmount.validate(BigDecimal("123.45")) shouldBe PositiveSubsidyAmount(BigDecimal(123.45))
    }

    "reject values with more than 2 decimal places" in {
      val exception = intercept[IllegalArgumentException] {
        PositiveSubsidyAmount.validate(BigDecimal("123.456"))
      }

      exception.getMessage shouldBe "123.456 is not a valid PositiveSubsidyAmount"
    }
  }

  "PositiveSubsidyAmount.apply" should {

    "return a tagged value for valid input" in {
      noException shouldBe thrownBy {
        PositiveSubsidyAmount(BigDecimal("100.00"))
      }
    }

    "accept invalid input" in {
      PositiveSubsidyAmount(BigDecimal("-1")).value shouldBe BigDecimal(-1)

    }
  }
}
