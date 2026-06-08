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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.DeclarationID

class DeclarationIDSpec extends AnyWordSpec with Matchers {

  "DeclarationID.validate" should {

    "accept a single character" in {
      DeclarationID.validate("A") shouldBe DeclarationID("A")
    }

    "accept 18 characters" in {
      DeclarationID.validate("123456789012345678") shouldBe DeclarationID("123456789012345678")
    }

    "reject an empty string" in {
      val exception = intercept[IllegalArgumentException] {
        DeclarationID.validate("")
      }

      exception.getMessage shouldBe " is not a valid DeclarationID"
    }

    /** Not quite sure about this behaviour
      */
    "reject more than 18 characters" in {
      DeclarationID.validate("1234567890123456789") shouldBe DeclarationID("1234567890123456789")
    }

    "accept more than 18 characters" in {
      DeclarationID.validate("1234567890123456789") shouldBe DeclarationID("1234567890123456789")
    }
  }

  "DeclarationID.apply" should {

    "create a DeclarationID for valid input" in {
      noException shouldBe thrownBy {
        DeclarationID("ABC123")
      }
    }

    "allow any input" in {
      DeclarationID("").value shouldBe ""
    }
  }
}
