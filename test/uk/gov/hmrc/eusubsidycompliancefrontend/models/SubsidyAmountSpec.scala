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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{MaxInputValue, SubsidyAmount}

class SubsidyAmountSpec extends AnyWordSpec with Matchers {

  "SubsidyAmount.validate" should {

    "accept zero" in {
      SubsidyAmount.validate(BigDecimal("0")) shouldBe SubsidyAmount(BigDecimal(0))
    }

    "accept positive values" in {
      SubsidyAmount.validate(BigDecimal("123.45")) shouldBe SubsidyAmount(BigDecimal(123.45))
    }

    "accept negative values" in {
      SubsidyAmount.validate(BigDecimal("-123.45")) shouldBe SubsidyAmount(BigDecimal(-123.45))
    }

    "accept the maximum value" in {
      SubsidyAmount.validate(MaxInputValue) shouldBe SubsidyAmount(BigDecimal(MaxInputValue))
    }

    "accept the minimum value" in {
      SubsidyAmount.validate(-MaxInputValue) shouldBe SubsidyAmount(BigDecimal(-MaxInputValue))
    }

    "reject values greater than the maximum" in {
      val exception = intercept[IllegalArgumentException] {
        SubsidyAmount.validate(MaxInputValue + 1)
      }

      exception.getMessage shouldBe "100000000000.99 is not a valid SubsidyAmount"
    }

    "reject values less than the minimum" in {
      val exception = intercept[IllegalArgumentException] {
        SubsidyAmount.validate(-MaxInputValue - 1)
      }

      exception.getMessage shouldBe "-100000000000.99 is not a valid SubsidyAmount"
    }

    "accept values with 2 decimal places" in {
      SubsidyAmount.validate(BigDecimal("123.45")) shouldBe SubsidyAmount(BigDecimal(123.45))
    }

    "reject values with more than 2 decimal places" in {
      val exception = intercept[IllegalArgumentException] {
        SubsidyAmount.validate(BigDecimal("123.456"))
      }

      exception.getMessage shouldBe "123.456 is not a valid SubsidyAmount"
    }
  }

  "SubsidyAmount.apply" should {

    "return a tagged value for valid input" in {
      noException shouldBe thrownBy {
        SubsidyAmount(BigDecimal("-100.00"))
      }
    }

    "allow invalid input" in {
      SubsidyAmount(MaxInputValue + 1).value shouldBe 100000000000.99
    }
  }
}
