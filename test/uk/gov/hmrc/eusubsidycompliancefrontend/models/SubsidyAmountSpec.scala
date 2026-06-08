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

  "SubsidyAmount.of" should {

    "accept zero" in {
      SubsidyAmount.of(BigDecimal("0")) shouldBe defined
    }

    "accept positive values" in {
      SubsidyAmount.of(BigDecimal("123.45")) shouldBe defined
    }

    "accept negative values" in {
      SubsidyAmount.of(BigDecimal("-123.45")) shouldBe defined
    }

    "accept the maximum value" in {
      SubsidyAmount.of(MaxInputValue) shouldBe defined
    }

    "accept the minimum value" in {
      SubsidyAmount.of(-MaxInputValue) shouldBe defined
    }

    "reject values greater than the maximum" in {
      SubsidyAmount.of(MaxInputValue + 1) shouldBe None
    }

    "reject values less than the minimum" in {
      SubsidyAmount.of(-MaxInputValue - 1) shouldBe None
    }

    "accept values with 2 decimal places" in {
      SubsidyAmount.of(BigDecimal("123.45")) shouldBe defined
    }

    "reject values with more than 2 decimal places" in {
      SubsidyAmount.of(BigDecimal("123.456")) shouldBe None
    }
  }

  "SubsidyAmount.apply" should {

    "return a tagged value for valid input" in {
      noException shouldBe thrownBy {
        SubsidyAmount(BigDecimal("-100.00"))
      }
    }

    "throw IllegalArgumentException for invalid input" in {
      intercept[IllegalArgumentException] {
        SubsidyAmount(MaxInputValue + 1)
      }
    }
  }

  "SubsidyAmount.Zero" should {

    "be a valid zero-valued SubsidyAmount" in {
      SubsidyAmount.Zero shouldBe SubsidyAmount(BigDecimal(0))
    }
  }
}
