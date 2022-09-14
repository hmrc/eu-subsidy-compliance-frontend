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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class EligibilityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with AuditServiceSupport
    with EscServiceSupport
    with EmailSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[AuditService].toInstance(mockAuditService),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService)
  )

  private val controller = instanceOf[EligibilityController]

  "EligibilityControllerSpec" when {
    val exception = new Exception("oh no!")

    "handling request to first empty page" must {

      def performAction() = controller
        .firstEmptyPage(
          FakeRequest("GET", routes.EligibilityController.firstEmptyPage().url)
            .withFormUrlEncodedBody()
        )

      "throw technical error" when {

        "call to get eligibility journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect to correct page" when {

        def redirect(eligibilityJourney: Option[EligibilityJourney], expectedRedirectLocation: String) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }
          checkIsRedirect(performAction(), expectedRedirectLocation)
        }

        "no eligibility journey present in store" in {
          redirect(None, routes.EligibilityController.getEoriCheck().url)
        }

        "no values are set in the eligibility journey" in {
          redirect(EligibilityJourney().some, routes.EligibilityController.getDoYouClaim().url)
        }

        "eligibility journey is complete" in {
          redirect(eligibilityJourney.some, routes.UndertakingController.firstEmptyPage().url)
        }

      }
    }

    "handling request to get do you claim" must {

      def performAction() = controller.getDoYouClaim(FakeRequest())

      "display the page" in {

        val eligibilityJourney = EligibilityJourney(eoriCheck = EoriCheckFormPage(true.some))

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNoEnrolmentNoCheck()
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.doYouClaim.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityController.getDoYouClaim().url

            }
          )
        }

        testDisplay(eligibilityJourney)
      }
    }

    "handling request to post do you claim" must {
      def performAction(data: (String, String)*) = controller
        .postDoYouClaim(
          FakeRequest("GET", routes.EligibilityController.getDoYouClaim().url)
            .withFormUrlEncodedBody(data: _*)
            .withHeaders("Referer" -> routes.EligibilityController.getDoYouClaim().url)
        )

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNoEnrolmentNoCheck()
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            messageFromMessageKey("customswaivers.error.required")
          )
        }
      }

      "redirect to next page" when {

        def testRedirection(inputValue: Boolean, nextCall: String) = {
          inSequence {
            mockAuthWithNoEnrolmentNoCheck()
          }
          checkIsRedirect(
            performAction("customswaivers" -> inputValue.toString),
            nextCall
          )
        }

        "Yes is selected" in {
          testRedirection(true, appConfig.eccEscSubscribeUrl)
        }

        "No is selected" in {
          testRedirection(false, routes.EligibilityController.getWillYouClaim().url)
        }

      }

    }

    "handling request to get will you claim" must {
      def performAction() = controller
        .getWillYouClaim(
          FakeRequest("GET", routes.EligibilityController.getWillYouClaim().url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {

        val previousUrl = routes.EligibilityController.getDoYouClaim().url

        inSequence {
          mockAuthWithNoEnrolmentNoCheck()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("willyouclaim.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
            val selectedOptions = doc.select(".govuk-radios__input[checked]")
            selectedOptions.isEmpty shouldBe true
            val button = doc.select("form")
            button.attr("action") shouldBe routes.EligibilityController.postWillYouClaim().url

          }
        )

      }

    }

    "handling request to post will you claim" must {

      def performAction(data: (String, String)*) = controller
        .postWillYouClaim(
          FakeRequest("GET", routes.EligibilityController.getWillYouClaim().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "show form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNoEnrolmentNoCheck()
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("willyouclaim.title"),
            messageFromMessageKey("willyouclaim.error.required")
          )
        }
      }

      "redirect to next page" when {

        def testRedirection(input: Boolean, nextCall: String) = {
          inSequence {
            mockAuthWithNoEnrolmentNoCheck()
          }
          checkIsRedirect(
            performAction("willyouclaim" -> input.toString),
            nextCall
          )
        }

        "Yes is selected" in {
          testRedirection(true, routes.EligibilityController.getEoriCheck().url)
        }

        "No is selected" in {
          testRedirection(false, routes.EligibilityController.getNotEligible().url)
        }

      }

    }

    "handling request to get not eligible" must {

      def performAction() = controller
        .getNotEligible(
          FakeRequest("GET", routes.EligibilityController.getNotEligible().url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithNoEnrolmentNoCheck()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("notEligible.title")
        )

      }
    }

    "handling request to get EORI check" must {

      def performAction() = controller
        .getEoriCheck(
          FakeRequest("GET", routes.EligibilityController.getEoriCheck().url)
            .withFormUrlEncodedBody()
        )

      "display the page" when {

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("eoricheck.title", eori1),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.eoriCheck.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true

              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityController.postEoriCheck().url

            }
          )
        }

        "user hasn't answered the question" in {
          testDisplay(EligibilityJourney())
        }

        "user has already answered the question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value :: $inputValue") {
              testDisplay(EligibilityJourney(eoriCheck = EoriCheckFormPage(inputValue.some)))
            }
          }
        }

      }

      "redirect to account home" when {
        "undertaking has already been created" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }

          val result = performAction()

          redirectLocation(result) should contain(routes.AccountController.getAccountPage().url)
        }
      }

    }

    "handling request to post EORI check" must {

      def performAction(data: (String, String)*) = controller
        .postEoriCheck(
          FakeRequest("POST", routes.EligibilityController.postEoriCheck().url)
            .withFormUrlEncodedBody(data: _*)
        )

      def update(ej: EligibilityJourney) = ej.copy(doYouClaim = DoYouClaimFormPage(true.some))

      "throw technical error" when {

        "eligibility journey fail to update" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(
              Left(ConnectorError(exception))
            )
          }
          assertThrows[Exception](await(performAction("eoricheck" -> "true")))
        }
      }

      "display form error" when {
        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("eoricheck.title", eori1),
            messageFromMessageKey("eoricheck.error.required")
          )
        }
      }

      "redirect to next page" when {

        val journey = EligibilityJourney(
          doYouClaim = DoYouClaimFormPage(Some(true))
        )

        def testRedirection(input: Boolean, nextCall: String) =
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(journey), eori1)(
              Right(journey.copy(eoriCheck = EoriCheckFormPage(input.some)))
            )

            checkIsRedirect(performAction("eoricheck" -> input.toString), nextCall)
          }

        "yes is selected" in {
          testRedirection(true, routes.AccountController.getAccountPage().url)
        }

        "no is selected" in {
          testRedirection(false, routes.EligibilityController.getIncorrectEori().url)
        }
      }

    }

    "handling request to get incorrect eori" must {

      def performAction() = controller
        .getIncorrectEori(
          FakeRequest("GET", routes.EligibilityController.getIncorrectEori().url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("incorrectEori.title")
        )

      }
    }

  }

}
