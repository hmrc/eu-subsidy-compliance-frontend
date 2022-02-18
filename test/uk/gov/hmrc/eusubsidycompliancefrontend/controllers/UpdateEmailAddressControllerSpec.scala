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

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.inject.bind
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Store



class UpdateEmailAddressControllerSpec extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour {

  override def overrideBindings           = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore)
  )

  override def additionalConfig = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(
        s"""
           | microservice.services.update-email {
           |       protocol = http
           |       host = localhost
           |       port = 9898
           |    }
           |""".stripMargin)
    )
  )

  val controller = instanceOf[UpdateEmailAddressController]

  "UpdateEmailAddressController" when {

    "handling request to update Email Address " must {

      def performAction() = controller.updateEmailAddress(FakeRequest())
      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("updateEmail.title"),
          { doc =>
            val button = doc.select("form")
            button.attr("action") shouldBe routes.UpdateEmailAddressController.postUpdateEmailAddress().url

          }
        )

      }
    }

    "handling request to post update email address" must {
      def performAction() = controller.postUpdateEmailAddress(FakeRequest())
      "redirect to next page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
        }
        val url = "http://localhost:9898/manage-email-cds/service/eu-subsidy-compliance-frontend"
        checkIsRedirect(performAction(), url )
      }
    }
  }

}
