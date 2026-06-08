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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TaxType

class TaxTypeSpec extends AnyWordSpec with Matchers {

  "TaxType.validate" should {

    "accept an empty string" in {
      TaxType.validate("") shouldBe TaxType("")
    }

    "accept a 3 character string" in {
      TaxType.validate("ABC") shouldBe TaxType("ABC")
    }

    "accept a string longer than 3 characters" in {
      TaxType.validate("ABCD") shouldBe TaxType("ABCD")
    }

    "accept a much longer string" in {
      TaxType.validate("abcdefghijklmnopqrstuvwxyz") shouldBe TaxType("abcdefghijklmnopqrstuvwxyz")
    }
  }
}
