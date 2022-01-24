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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, FormPage, Store}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessEntityControllerSpec  extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour {

  val mockEscService = mock[EscService]

  override def overrideBindings           = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  val controller = instanceOf[BusinessEntityController]

  def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  "BusinessEntityControllerSpec" when {

    "handling request to get add Business Page" must {

      def performAction() = controller.getAddBusinessEntity(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BusinessEntityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRetreiveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking retrieve no undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))

          }
          assertThrows[Exception](await(performAction()))
        }

        "call to store undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockPut[Undertaking](undertaking, eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        def test(input: Option[String], businessEntityJourney: BusinessEntityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockPut[Undertaking](undertaking, eori1)(Right(undertaking))
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("addBusiness.businesses-added.title", undertaking.name),
            {doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              input match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty       shouldBe true
              }


              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postAddBusinessEntity.url
            }
          )
        }

        "user hasn't already answered the question" in {
         test(None, BusinessEntityJourney())
        }

        "user has already answered the question" in {
          test(Some("true"), BusinessEntityJourney(addBusiness = FormPage("add-member", true.some)))
        }
      }

    }

    "handling request to post add Business Page" must {

      def performAction(data: (String, String)*) = controller.postAddBusinessEntity(FakeRequest("POST",routes.BusinessEntityController.getAddBusinessEntity().url).withFormUrlEncodedBody(data: _*))

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to fetch undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking passes but the undertaking came back as None" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update BusinessEntityJourney fails" in {

          def updateFunc(beOpt: Option[BusinessEntityJourney]) =
            beOpt.map(x => x.copy(addBusiness  = x.addBusiness.copy(value = Some(true))))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Right(undertaking.some))
            mockUpdate[BusinessEntityJourney](_ => updateFunc(businessEntityJourney.some), eori)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("addBusiness" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String) = {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Right(undertaking.some))
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("addBusiness.title", undertaking.name),
            messageFromMessageKey(errorMessage, undertaking.name)
          )
        }

        "nothing has been submitted" in {
          displayErrorTest()("addBusiness.error.required")
        }


      }

      "redirect to the next page" when {

        "user selected No" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Right(undertaking.some))
          }
          checkIsRedirect(performAction("addBusiness" -> "false"), routes.AccountController.getAccountPage().url)
        }

        "user selected Yes" in {
          def updateFunc(beOpt: Option[BusinessEntityJourney]) =
            beOpt.map(x => x.copy(addBusiness  = x.addBusiness.copy(value = Some(true))))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori)(Right(undertaking.some))
            mockUpdate[BusinessEntityJourney](_ => updateFunc(BusinessEntityJourney().some), eori)(Right(BusinessEntityJourney(addBusiness = FormPage("add-member", true.some))))
          }
          checkIsRedirect(performAction("addBusiness" -> "true"), "add-business-entity-eori")
        }

      }


    }

  }

}
