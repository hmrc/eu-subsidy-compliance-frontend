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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.Configuration
import play.api.test.FakeRequest

class SignOutControllerSpec extends ControllerSpec {

  private val controller = instanceOf[SignOutController]

  override def additionalConfig = Configuration.from(
    Map(
      // Disable CSP n=once hashes in rendered output
      "urls.timeOutContinue" -> "http://host:123/continue"
    )
  )

  "BusinessEntityControllerSpec" when {

    "handling request to signOut From Timeout" must {

      def performAction() = controller.signOutFromTimeout(FakeRequest())

      "display the page" in {

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("timedOut.title"),
          { doc =>
            val body = doc.select(".govuk-body").text()
            body should include regex messageFromMessageKey("timedOut.p1")
            body should include regex messageFromMessageKey("timedOut.signIn", appConfig.timeOutContinue)
          }
        )

      }
    }

    "handling request to get sign out" must {

      def performAction() = controller.signOut(FakeRequest())

      "display the page" in {

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("signOut.title"),
          doc => doc.select(".govuk-body").text() shouldBe "You have been signed out of this session."
        )

      }
    }
  }
}
