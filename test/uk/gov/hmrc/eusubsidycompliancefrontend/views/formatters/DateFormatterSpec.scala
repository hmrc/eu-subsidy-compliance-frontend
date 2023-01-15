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

import org.mockito.ArgumentMatchers.{any, eq => mockitoEq}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.fixedDate

class DateFormatterSpec extends AnyWordSpecLike with MockitoSugar with Matchers {

  "TimeUtils" when {

    "govDisplayFormat is called" should {
      "return a correctly formatted date string" in {
        // Using mockito since scalamock doesn't seem able to handle the apply methods on the Messages trait.
        val mockMessages = mockMessagesFor("date.1", "January")
        DateFormatter.govDisplayFormat(fixedDate)(mockMessages) shouldBe "20 January 2021"
      }
    }

    "govDisplayFormatTruncated is called" should {
      "return a correct formatted date string with truncated month name" in {
        val mockMessages = mockMessagesFor("date.truncated.1", "Jan")
        DateFormatter.govDisplayFormatTruncated(fixedDate)(mockMessages) shouldBe "20 Jan 2021"
      }
    }
  }

  // Using mockito since scalamock doesn't seem able to handle the apply methods on the Messages trait.
  private def mockMessagesFor(key: String, value: String) = {
    val mockMessages = mock[Messages]
    when(mockMessages.apply(mockitoEq(key), any())).thenReturn(value)
    mockMessages
  }

}
