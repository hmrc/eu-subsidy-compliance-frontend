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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI

class EORISpec extends AnyWordSpec with Matchers {

  // -------------------------
  // VALIDATION TESTS
  // -------------------------

  "EORI.from" should {

    "accept a valid GB EORI" in {
      EORI.from("GB100823993327") shouldBe defined
    }

    "accept valid GB EORI with uppercase prefix and 12 digits" in {
      EORI.from("GB123456789012") shouldBe defined
    }

    "accept valid GB EORI with 15 digits" in {
      EORI.from("GB123456789012345") shouldBe defined
    }

    "accept valid XI EORI with 12 digits" in {
      EORI.from("XI123456789012") shouldBe defined
    }

    "accept lowercase gb prefix due to regex" in {
      EORI.from("gb123456789012") shouldBe defined
    }

    "accept mixed-case prefix due to regex" in {
      EORI.from("gB123456789012") shouldBe defined
    }

    "reject missing prefix" in {
      EORI.from("123456789012") shouldBe None
    }

    "reject invalid prefix" in {
      EORI.from("US123456789012") shouldBe None
    }

    "reject too few digits" in {
      EORI.from("GB1234567890") shouldBe None
    }

    "reject too many digits" in {
      EORI.from("GB1234567890123456") shouldBe None
    }

    "reject letters in numeric section" in {
      EORI.from("GB12345ABC9012") shouldBe None
    }

    "reject empty string" in {
      EORI.from("") shouldBe None
    }

    // IMPORTANT: catches findFirstIn bug behaviour
    "reject string containing a valid EORI as substring (critical)" in {
      val input = "XXXGB123456789012YYY"
      EORI.from(input) shouldBe None
    }
  }

  // -------------------------
  // APPLY BEHAVIOUR
  // -------------------------

  "EORI.apply" should {

    "return tagged value for valid EORI" in {
      noException shouldBe thrownBy {
        EORI("GB123456789012")
      }
    }

    "throw IllegalArgumentException for invalid EORI" in {
      intercept[IllegalArgumentException] {
        EORI("INVALID")
      }
    }
  }

  // -------------------------
  // PREFIX UTILITY
  // -------------------------

  "EORI.withGbPrefix" should {

    "not double-prefix GB" in {
      EORI.withGbPrefix("GB123456789012") shouldBe "GB123456789012"
    }

    "add GB prefix when missing" in {
      EORI.withGbPrefix("123456789012") shouldBe "GB123456789012"
    }

    "handle lowercase input without normalising" in {
      EORI.withGbPrefix("gb123456789012") shouldBe "GBgb123456789012"
    }

    "not validate input, only prefix" in {
      EORI.withGbPrefix("ABC") shouldBe "GBABC"
    }
  }

  // -------------------------
  // FORMATTING FUNCTION
  // -------------------------

  "EORI.formatEori" should {

    "remove spaces and normalise prefix case" in {
      EORI.formatEori("gb 123456789012") shouldBe "GB123456789012"
    }

    "uppercase first two letters only" in {
      EORI.formatEori("xi123456789012") shouldBe "XI123456789012"
    }

    "preserve numeric section" in {
      EORI.formatEori("gb123456789012") shouldBe "GB123456789012"
    }

    "handle already formatted input idempotently" in {
      EORI.formatEori("GB123456789012") shouldBe "GB123456789012"
    }

    "not validate length or structure" in {
      EORI.formatEori("xx") shouldBe "XX"
    }
  }

  // -------------------------
  // BOUNDARY TESTS (IMPORTANT)
  // -------------------------

  "EORI validation boundaries" should {

    "accept minimum valid GB length (14 chars)" in {
      EORI.from("GB" + "1" * 12) shouldBe defined
    }

    "accept maximum valid GB length (17 chars)" in {
      EORI.from("GB" + "1" * 15) shouldBe defined
    }

    "reject 13-digit numeric part" in {
      EORI.from("GB" + "1" * 11) shouldBe None
    }

    "reject 16-digit numeric part" in {
      EORI.from("GB" + "1" * 16) shouldBe None
    }
  }

  // -------------------------
  // LOG MASKING
  // -------------------------

  "EORI.mask" should {

    "mask the middle digits of a 12 digit GB EORI" in {
      EORI.mask("GB455655007000") shouldBe "GB45565XXX7000"
    }

    "mask the middle digits of a 15 digit XI EORI" in {
      EORI.mask("XI123456789012345") shouldBe "XI12345XXXXXX2345"
    }

    "mask every EORI inside a log message" in {
      val msg =
        "returned Some(Undertaking(NI050001885,Test2 Ltd,List(BusinessEntity(GB250822064064,false), BusinessEntity(GB455655007000,true)))) for  EORI 'GB455655007000'"

      EORI.mask(msg) shouldBe
        "returned Some(Undertaking(NI050001885,Test2 Ltd,List(BusinessEntity(GB25082XXX4064,false), BusinessEntity(GB45565XXX7000,true)))) for  EORI 'GB45565XXX7000'"
    }

    "leave text without an EORI unchanged" in {
      EORI.mask("UndertakingRef:NI050001885 GB12345") shouldBe "UndertakingRef:NI050001885 GB12345"
    }

    "expose masked as an extension on EORI" in {
      EORI("GB455655007000").masked shouldBe "GB45565XXX7000"
    }
  }

  // -------------------------
  // REGEX IMPLEMENTATION SAFETY CHECK
  // -------------------------

  "EORI regex behaviour" should {

    "NOT match substrings inside longer strings" in {
      val input = "randomGB123456789012random"

      // This SHOULD be None in correct implementation
      EORI.from(input) shouldBe None
    }
  }
}
