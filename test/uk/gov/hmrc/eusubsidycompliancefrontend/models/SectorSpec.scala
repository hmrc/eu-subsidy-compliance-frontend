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

package uk.gov.hmrc.eusubsidycompliancefrontend.models
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec

class SectorSpec extends BaseSpec with Matchers {

  "Sector" when {
    "agriculture" must {
      "return two as sector value" in {
        Sector.agriculture.toString mustBe "3"
      }
    }

    "aquaculture" must {
      "return three as sector value" in {
        Sector.aquaculture.toString mustBe "2"
      }
    }

    "transport" must {
      "return one as sector value" in {
        Sector.transport.toString mustBe "1"
      }
    }

    "other" must {
      "return zero as sector value" in {
        Sector.other.toString mustBe "0"
      }
    }
  }

}
