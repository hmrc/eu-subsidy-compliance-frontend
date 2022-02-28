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
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, Store}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{businessEntityJourney1, eori1, undertaking1}

import scala.concurrent.Future

class NoBusinessPresentControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour {

  val mockEscService = mock[EscService]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  val controller = instanceOf[NoBusinessPresentController]

  def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  "NoBusinessPresentControllerSpec" when {

    "handling request to get No Business Present" must {
      def performAction() = controller.getNoBusinessPresent(FakeRequest())

      behave like authBehaviour(() => performAction())

      " throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to retrieve undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(Future.successful(undertaking1.some))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("noBusinessPresent.title", undertaking1.name),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe (routes.AccountController.getAccountPage().url)
            val button = doc.select("form")
            button.attr("action") shouldBe routes.NoBusinessPresentController.postNoBusinessPresent().url
          }
        )
      }
    }

    "handling request to post No Business Pesent" must {
      def performAction() = controller.postNoBusinessPresent(FakeRequest())

      def update(businessEntityOpt: Option[BusinessEntityJourney]) =
        businessEntityOpt.map(_.copy(isLeadSelectJourney = true.some))

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to update business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney1.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "redirect to next page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney1.some), eori1)(
            Right(businessEntityJourney1)
          )
        }
        checkIsRedirect(
          performAction(),
          routes.BusinessEntityController.getAddBusinessEntity().url
        )
      }
    }

  }

}
