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

import cats.implicits.catsSyntaxOptionId
import play.api.inject.bind
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, EscService, JourneyStore, Store, UndertakingJourney}
import utils.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global

class AccountControllerSpec  extends ControllerSpec
with AuthSupport
with JourneyStoreSupport
with AuthAndSessionDataBehaviour {
  val mockEscService = mock[EscService]

  override def overrideBindings           = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
      bind[EscService].toInstance(mockEscService)
  )

  val controller = instanceOf[AccountController]

  "AccountControllerSpec" when {

    "handling request to get Account page" must {

      def performAction() = controller.getAccountPage(FakeRequest())

      behave like authBehaviour(() => performAction())

      "display the page" in {

        inSequence {
          mockRetreiveUndertaking(eori1)
          mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          mockGet[BusinessEntityJourney](eori1)(Right(businessEntity1).some)

        }

      }

    }

  }

}
