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

class TaxTypeSpec extends AnyWordSpec with Matchers{

  "TaxType.of" should {

    "accept an empty string" in {
      TaxType.of("") shouldBe defined
    }

    "accept a 3 character string" in {
      TaxType.of("ABC") shouldBe defined
    }

    "accept a string longer than 3 characters" in {
      TaxType.of("ABCD") shouldBe defined
    }

    "accept a much longer string" in {
      TaxType.of("abcdefghijklmnopqrstuvwxyz") shouldBe defined
    }
  }
}
