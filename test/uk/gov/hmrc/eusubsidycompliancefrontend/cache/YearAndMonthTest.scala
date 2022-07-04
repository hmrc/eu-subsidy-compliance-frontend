/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.cache

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class YearAndMonthTest extends AnyWordSpec with Matchers {

  "YearAndMonth" should {

    "generate the expected string representation when toString is called" in {
      val underTest = YearAndMonth(2001, 12)
      underTest.toString shouldBe "2001-12"
    }

    "pad the year and month values correctly" in {
      val underTest = YearAndMonth(1, 1)
      underTest.toString shouldBe "0001-01"
    }
  }

}
