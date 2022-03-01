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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.TermsAndConditionsAccepted
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import utils.CommonTestData._

class EligibilityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with JourneySupport
    with AuditServiceSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService),
    bind[AuditService].toInstance(mockAuditService)
  )

  val controller = instanceOf[EligibilityController]

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
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility journey passes but come back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect to next page" when {

        def redirect(eligibilityJourney: EligibilityJourney, nextCall: String) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          }
          checkIsRedirect(performAction(), nextCall)
        }

        "first empty value comes out to be empty" in {
          redirect(EligibilityJourney(), "/do-you-claim-customs-waivers")
        }

        "Eligibility journey is complete" in {
          redirect(eligibilityJourney, routes.UndertakingController.getUndertakingName().url)
        }

      }
    }

    "handling request to get custom waivers" must {

      def performAction() = controller
        .getCustomsWaivers(FakeRequest().withFormUrlEncodedBody())

      "throw technical error" when {

        "call to get eligibility journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility journey passes but come back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.customsWaivers.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityController.postCustomsWaivers().url

            }
          )
        }

        "user hasn't answered the question" in {
          testDisplay(EligibilityJourney())
        }

        "user has already answered te question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value:: $inputValue") {
              testDisplay(EligibilityJourney(customsWaivers = CustomsWaiversFormPage(inputValue.some)))
            }

          }

        }

      }
    }

    "handling request to post custom waivers" must {
      def performAction(data: (String, String)*) = controller
        .postCustomsWaivers(
          FakeRequest("GET", routes.EligibilityController.getCustomsWaivers().url)
            .withFormUrlEncodedBody(data: _*)
        )

      val eligibilityJourney = EligibilityJourney()

      def update(eligibilityJourneyOpt: Option[EligibilityJourney]) = eligibilityJourneyOpt
        .map(_.copy(customsWaivers = CustomsWaiversFormPage(true.some)))

      "throw technical error" when {

        "call to update eligibility journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("customswaivers" -> "true")))
        }
      }

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            messageFromMessageKey("customswaivers.error.required")
          )
        }
      }

      "redirect to next page" in {

        List("true", "false").foreach { inputValue =>
          withClue(s" For input value :: $inputValue") {
            inSequence {
              mockAuthWithNecessaryEnrolment()
              mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(
                Right(EligibilityJourney(customsWaivers = CustomsWaiversFormPage(true.some)))
              )
            }
            checkIsRedirect(performAction("customswaivers" -> inputValue), "/main-business-check")
          }
        }
      }
    }

    "handling request to get will you claim" must {
      def performAction() = controller
        .getWillYouClaim(
          FakeRequest("GET", routes.EligibilityController.getWillYouClaim().url)
            .withFormUrlEncodedBody()
        )

      "throw technical error" when {

        "call to get eligibility fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility passes but fetches nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        val previousUrl = "/do-you-claim-customs-waivers"

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("willyouclaim.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.willYouClaim.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityController.postWillYouClaim().url

            }
          )
        }

        "user hasn't answered the question" in {
          testDisplay(EligibilityJourney())
        }

        "user has already answered the question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value :: $inputValue") {
              testDisplay(EligibilityJourney(willYouClaim = WillYouClaimFormPage(inputValue.some)))
            }
          }
        }
      }
    }

    "handling request to post will you claim" must {

      def performAction(data: (String, String)*) = controller
        .postWillYouClaim(
          FakeRequest("GET", routes.EligibilityController.getWillYouClaim().url)
            .withFormUrlEncodedBody(data: _*)
        )

      val previousUrl = routes.EligibilityController.getCustomsWaivers().url

      val eligibilityJourney = EligibilityJourney(
        willYouClaim = WillYouClaimFormPage(false.some)
      )
      val updatedEligibilityJourney = eligibilityJourney.copy(willYouClaim = WillYouClaimFormPage(true.some))

      def update(ejOpt: Option[EligibilityJourney]) = ejOpt
        .map(ej => ej.copy(willYouClaim = ej.willYouClaim.copy(value = true.some)))

      "throw technical error" when {

        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get update eligibility journey fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious(eori)(Right(previousUrl))
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("willyouclaim" -> "true")))
        }

      }

      "show form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori)(Right(previousUrl))
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("willyouclaim.title"),
            messageFromMessageKey("willyouclaim.error.required")
          )
        }
      }

      "redirect to next page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGetPrevious(eori)(Right(previousUrl))
          mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Right(updatedEligibilityJourney))
        }

        checkIsRedirect(performAction("willyouclaim" -> "true"), "/main-business-check")
      }
    }

    "handling request to get main business check" must {

      def performAction() = controller
        .getMainBusinessCheck(
          FakeRequest("GET", routes.EligibilityController.getMainBusinessCheck().url)
            .withFormUrlEncodedBody()
        )

      "throw technical error" when {

        "call to get eligibility fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility passes but fetches nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        val previousUrl = "/not-eligible"

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("mainbusinesscheck.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.mainBusinessCheck.value match {
                case Some(value) =>
                  if (value) {
                    doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
                  } else {
                    doc.select(".govuk-back-link").attr("href") shouldBe "/not-eligible"
                    selectedOptions.attr("value") shouldBe value.toString
                  }

                case None =>
                  selectedOptions.isEmpty shouldBe true
                  doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityController.postMainBusinessCheck().url

            }
          )
        }

        "user hasn't answered the question" in {
          testDisplay(EligibilityJourney(mainBusinessCheck = MainBusinessCheckFormPage(true.some)))
        }

        "user has already answered the question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value :: $inputValue") {
              testDisplay(EligibilityJourney(mainBusinessCheck = MainBusinessCheckFormPage(inputValue.some)))
            }
          }
        }

      }
    }

    "handling request to post main business check" must {

      def performAction(data: (String, String)*) = controller
        .postMainBusinessCheck(
          FakeRequest("GET", routes.EligibilityController.getMainBusinessCheck().url)
            .withFormUrlEncodedBody(data: _*)
        )

      val previousUrl = routes.EligibilityController.getWillYouClaim().url
      val eligibilityJourney = EligibilityJourney(
        customsWaivers = CustomsWaiversFormPage(false.some)
      )
      val updatedEligibilityJourney =
        eligibilityJourney.copy(mainBusinessCheck = MainBusinessCheckFormPage(true.some))

      def update(ejOpt: Option[EligibilityJourney]) = ejOpt
        .map(ej => ej.copy(mainBusinessCheck = ej.mainBusinessCheck.copy(value = true.some)))

      "throw technical error" when {

        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get update eligibility journey fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious(eori)(Right(previousUrl))
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("mainbusinesscheck" -> "true")))
        }

      }

      "show form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori)(Right(previousUrl))
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("mainbusinesscheck.title"),
            messageFromMessageKey("mainbusinesscheck.error.required")
          )
        }
      }

      "redirect to next page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGetPrevious(eori)(Right(previousUrl))
          mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Right(updatedEligibilityJourney))
        }
        checkIsRedirect(performAction("mainbusinesscheck" -> "true"), "/terms-conditions")
      }
    }

    "handling request to get terms" must {

      def performAction() = controller
        .getTerms(
          FakeRequest("GET", routes.EligibilityController.getTerms().url)
            .withFormUrlEncodedBody()
        )

      "throw technical error" when {

        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" in {

        val previousUrl = routes.EligibilityController.getNotEligible().url
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGetPrevious[EligibilityJourney](eori1)(Right(previousUrl))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("eligibilityTerms.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
            val button = doc.select("form")
            button.attr("action") shouldBe routes.EligibilityController.postTerms().url

          }
        )
      }

    }

    "handling request to post terms" must {

      def performAction(data: (String, String)*) = controller
        .postTerms(
          FakeRequest("POST", routes.EligibilityController.getTerms().url)
            .withFormUrlEncodedBody(data: _*)
        )

      val eligibilityJourney = EligibilityJourney(
        customsWaivers = CustomsWaiversFormPage(true.some),
        willYouClaim = WillYouClaimFormPage(true.some),
        notEligible = NotEligibleFormPage(true.some),
        mainBusinessCheck = MainBusinessCheckFormPage(true.some),
        signOut = SignOutFormPage(true.some)
      )

      def update(ejOpt: Option[EligibilityJourney]) = ejOpt
        .map(ej => ej.copy(acceptTerms = ej.acceptTerms.copy(value = true.some)))

      "throw technical error" when {

        "call to update eligibility journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("terms" -> "true")))
        }
      }

      "redirect to next page" in {
        val expectedAuditEvent = TermsAndConditionsAccepted(eori1)
        val updatedJourney = eligibilityJourney.copy(acceptTerms = AcceptTermsFormPage(true.some))
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockUpdate[EligibilityJourney](_ => update(eligibilityJourney.some), eori1)(Right(updatedJourney))
          mockSendAuditEvent(expectedAuditEvent)
        }
        checkIsRedirect(performAction("terms" -> "true"), "/eoricheck")
      }

    }

    "handling request to get EORI check" must {

      def performAction() = controller
        .getEoriCheck(
          FakeRequest("GET", routes.EligibilityController.getEoriCheck().url)
            .withFormUrlEncodedBody()
        )

      "throw technical error" when {
        "call to get eligibility fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility passes but fetches nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        val previousUrl = "/terms-conditions"

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("eoricheck.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
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
          testDisplay(EligibilityJourney(acceptTerms = AcceptTermsFormPage(true.some)))
        }

        "user has already answered the question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value :: $inputValue") {
              testDisplay(
                EligibilityJourney(
                  acceptTerms = AcceptTermsFormPage(true.some),
                  eoriCheck = EoriCheckFormPage(inputValue.some)
                )
              )
            }
          }
        }

      }
    }

  }
}
