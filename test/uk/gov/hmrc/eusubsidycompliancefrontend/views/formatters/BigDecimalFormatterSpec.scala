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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters

import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec

class BigDecimalFormatterSpec extends BaseSpec with Matchers {

  "BigDecimalFormatter" when {

    "toEuros is called" should {

      "return a formatted string containing the value formatted to two decimal places" in {
        BigDecimalFormatter.toEuros(BigDecimal(1)) shouldBe "€1.00"
      }

      "format numbers greater than 999.99 with comma separators" in {
        BigDecimalFormatter.toEuros(BigDecimal(1000)) shouldBe "€1,000.00"
        BigDecimalFormatter.toEuros(BigDecimal(1000000)) shouldBe "€1,000,000.00"
      }

      "round sub cent amounts down" in {
        BigDecimalFormatter.toEuros(BigDecimal(1.457)) shouldBe "€1.45"
        BigDecimalFormatter.toEuros(BigDecimal(1.452)) shouldBe "€1.45"
      }

    }

    "toPounds is called" should {

      "return a formatted string containing the value formatted to two decimal places" in {
        BigDecimalFormatter.toPounds(BigDecimal(1)) shouldBe "£1.00"
      }

      "format numbers greater than 999.99 with comma separators" in {
        BigDecimalFormatter.toPounds(BigDecimal(1000)) shouldBe "£1,000.00"
        BigDecimalFormatter.toPounds(BigDecimal(1000000)) shouldBe "£1,000,000.00"
      }

      "round sub cent amounts down" in {
        BigDecimalFormatter.toPounds(BigDecimal(1.457)) shouldBe "£1.45"
        BigDecimalFormatter.toPounds(BigDecimal(1.452)) shouldBe "£1.45"
      }

    }

  }

}
