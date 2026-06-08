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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TraderRef


class TraderRefSpec extends AnyWordSpec with Matchers {

  "TraderRef.of" should {

   "accept an empty string" in {
      TraderRef.of("") shouldBe None
    }

    "accept a single character" in {
      TraderRef.of("A") shouldBe defined
    }

    "accept 400 characters" in {
      TraderRef.of("A" * 400) shouldBe defined
    }

    "accept more than 400 characters" in {
      TraderRef.of("A" * 401) shouldBe defined
    }

  }

}
