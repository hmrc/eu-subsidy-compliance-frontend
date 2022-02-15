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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Error, OptionalEORI}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{ EscService, FormPage, JourneyTraverseService, Store, SubsidyJourney}
import utils.CommonTestData.{subsidyJourney, _}

class SubsidyControllerSpec extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour
  with JourneySupport {

  val mockEscService = mock[EscService]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService)
  )

  val controller = instanceOf[SubsidyController]

  "SubsidyControllerSpec" when {

    "handling request to get Add Claim Eori " must {

      def performAction() = controller
        .getAddClaimEori(FakeRequest("GET",routes.SubsidyController.getAddClaimEori().url))

      "throw technical error" when {
        val exception = new Exception("oh no")
        " the call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        " the call to get subsidy journey comes back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney) ={
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-eori.title"),
            {doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.addClaimEori.value match {
                case Some(OptionalEORI(input, eori)) => {
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe eori.map(_.drop(2)).getOrElse("")
                }
                case _ => selectedOptions.isEmpty       shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimEori().url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(subsidyJourney.copy(addClaimEori = FormPage("add-claim-eori", None)))

        }

        "the user has already answered the question" in {
          List(subsidyJourney,
            subsidyJourney.copy(addClaimEori = FormPage("add-claim-eori", OptionalEORI("false", None).some)))
            .foreach { subsidyJourney =>
              withClue(s" for each subsidy journey $subsidyJourney") {
                test(subsidyJourney)
              }

            }

        }

      }
    }

    "handling the request to post add claim eori" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimEori(
          FakeRequest("POST",routes.SubsidyController.getAddClaimEori().url)
            .withFormUrlEncodedBody(data: _*))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update subsidy journey fails" in {

          def update(subsidyJourneyOpt: Option[SubsidyJourney]) =
            subsidyJourneyOpt.map(_.copy(addClaimEori = FormPage("add-claim-eori", OptionalEORI("false", None).some)))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-amount"))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.copy(addClaimEori = FormPage("add-claim-eori", None)).some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("should-claim-eori" -> "false")))
        }
      }

      "show form error" when {

        def testFormError(
                          inputAnswer: Option[List[(String, String)]],
                          errorMessageKey: String) = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-amount"))
          }
          checkFormErrorIsDisplayed(
            performAction(answers: _*),
            messageFromMessageKey("add-claim-eori.title"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "add-claim-eori.error.required")
        }

        "yes is selected but no eori is entered" in {
          testFormError(Some(List("should-claim-eori" -> "true")), "add-claim-eori.error.format")

        }

      }

      "redirect to next page" when {

        def update(subsidyJourneyOpt: Option[SubsidyJourney], formValues: Option[OptionalEORI]) =
          subsidyJourneyOpt.map(_.copy(addClaimEori = FormPage("add-claim-eori", formValues)))

        def testRedirect(optionalEORI: OptionalEORI, inputAnswer: List[(String, String)]) ={
          val updatedSubsidyJourney = update(subsidyJourney.some, optionalEORI.some).getOrElse(sys.error(" no subsdy journey"))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-amount"))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some, optionalEORI.some), eori1)(Right(updatedSubsidyJourney))
          }
          checkIsRedirect(performAction(inputAnswer: _*), "add-claim-public-authority")
        }

        "user selected yes and enter eori number" in {
          testRedirect(OptionalEORI("true", "123456789013".some), List("should-claim-eori" -> "true", "claim-eori" -> "123456789013"))
        }

        "user selected No " in {
          testRedirect(OptionalEORI("false", None), List("should-claim-eori" -> "false"))
        }

      }

    }
  }


}
