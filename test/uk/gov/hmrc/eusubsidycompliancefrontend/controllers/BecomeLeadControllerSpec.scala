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
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData._

import scala.concurrent.Future

class BecomeLeadControllerSpec
  extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour {

  val mockEscService = mock[EscService]

  override def overrideBindings           = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  val controller = instanceOf[BecomeLeadController]

  def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  "BecomeLeadControllerSpec" when {

    "handling request to get Become Lead Eori" must {

      def performAction() = controller.getBecomeLeadEori(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" when {

        "new lead journey is blank " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Right(None))
            mockPut[BecomeLeadJourney](newBecomeLeadJourney, eori)(Right(newBecomeLeadJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            {doc =>

              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty  shouldBe true

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BecomeLeadController.postBecomeLeadEori().url
            }
          )

        }
        "new lead journey already exists" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true))).some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            {doc =>

              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.attr("value") shouldBe "true"

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BecomeLeadController.postBecomeLeadEori().url
            }
          )
        }
      }

    }

    "handling request to post Become Lead Eori" must {

      def performAction(data: (String, String)*) = controller
        .postBecomeLeadEori(
          FakeRequest().withFormUrlEncodedBody(data: _*))

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to update new lead journey fails" in {

          def update(newLeadJourneyOpt: Option[BecomeLeadJourney]) = {
            newLeadJourneyOpt.map(_.copy(becomeLeadEori = FormPage("select-new-lead")))
          }
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[BecomeLeadJourney](_ => update(BecomeLeadJourney().some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("becomeAdmin" -> "true")))
        }

      }

    }

    "handling request to get Become Lead Eori Terms" must {

      def performAction() = controller.getAcceptPromotionTerms(FakeRequest())
      behave like authBehaviour(() => performAction())

      def update(businessEntityJourneyOpt: Option[BusinessEntityJourney]) = {
        businessEntityJourneyOpt.map(_.copy(isLeadSelectJourney = None))
      }

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true))).some))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("become-admin-terms-and-conditions.title")
        )

      }

    }

  }

}
