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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, FormPage, NewLeadJourney, Store}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData._

import scala.concurrent.Future

class SelectNewLeadControllerSpec
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

  val controller = instanceOf[SelectNewLeadController]

  def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  "SelectNewLeadControllerSpec" when {

    "handling request to get Select New Lead" must {

      def performAction() = controller.getSelectNewLead(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to retrieve undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Right(NewLeadJourney().some))
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to retrieve undertaking came back with empty response" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Right(NewLeadJourney().some))
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to put New lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Right(None))
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        "new lead journey is blank " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Right(None))
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Right(NewLeadJourney()))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title", undertaking.name),
            {doc =>

              doc.select(".govuk-back-link").attr("href") shouldBe(routes.AccountController.getAccountPage().url)
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty  shouldBe true

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SelectNewLeadController.postSelectNewLead().url
            }
          )

        }
        "new lead journey already exists" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking1.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title", undertaking1.name),
            {doc =>

              doc.select(".govuk-back-link").attr("href") shouldBe(routes.AccountController.getAccountPage().url)
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.attr("value") shouldBe eori4.toString

              val radioOptions = doc.select(".govuk-radios__item")
              radioOptions.size() shouldBe(undertaking1.undertakingBusinessEntity.filterNot(_.leadEORI).size)

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SelectNewLeadController.postSelectNewLead().url
            }
          )

        }
      }

    }

    "handling request to post select new lead" must {

      def performAction(data: (String, String)*) = controller
        .postSelectNewLead(
          FakeRequest().withFormUrlEncodedBody(data: _*))

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to retrieve Undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4.toString)))
        }

        "call to retrieve Undertaking came back with Nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4.toString)))
        }

        "call to update new lead journey fails" in {

          def update(newLeadJourneyOpt: Option[NewLeadJourney]) = {
            newLeadJourneyOpt.map(_.copy(selectNewLead = FormPage("select-new-lead", eori4.some)))
          }
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney().some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4.toString)))
        }

      }

      "show the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking1.some))
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title", undertaking.name),
            messageFromMessageKey("selectNewLead.error.required")
          )
        }
      }


    }

  }

}
