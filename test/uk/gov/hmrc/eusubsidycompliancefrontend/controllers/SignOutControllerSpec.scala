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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class SignOutControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with EmailSupport
    with TimeProviderSupport
    with AuditServiceSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector)
  )

  private val controller = instanceOf[SignOutController]

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString("""
          |"urls.timeOutContinue" = "http://host:123/continue"
          |""".stripMargin)
    )
  )

  "SignOutController" when {

    "handling request to signOut From Timeout" must {
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

        def performAction() = controller.signOut(
          FakeRequest(GET, routes.SignOutController.signOut().url)
        )

        "redirect to the signout URL" when {

          def testRedirect(): Unit = {
            inSequence {
              mockAuthWithEnrolment(eori4)
            }

            val result = performAction()

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(appConfig.signOutUrl(Some(appConfig.exitSurveyUrl)))
          }

          "for a valid request" in {
            testRedirect()
          }

        }

      }

    }
  }
}
