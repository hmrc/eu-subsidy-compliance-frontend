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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.TermsAndConditionsAccepted
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class EligibilityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with JourneySupport
    with AuditServiceSupport
    with EmailSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService),
    bind[AuditService].toInstance(mockAuditService),
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
          redirect(EligibilityJourney(), routes.EligibilityController.getEoriCheck().url)
        }

        "Eligibility journey is complete" in {
          redirect(eligibilityJourney, routes.UndertakingController.getUndertakingName().url)
        }

      }
    }

    "handling request to get custom waivers" must {

      def performAction() = controller
        .getCustomsWaivers(
          FakeRequest()
        )

      "display the page" in {

        val eligibilityJourney = EligibilityJourney(eoriCheck = EoriCheckFormPage(true.some))

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNoEnrolment()
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

        testDisplay(eligibilityJourney)
      }
    }

    "handling request to post custom waivers" must {
      def performAction(data: (String, String)*) = controller
        .postCustomsWaivers(
          FakeRequest("GET", routes.EligibilityController.getCustomsWaivers().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNoEnrolment()
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
            mockAuthWithNoEnrolment()
          }
          checkIsRedirect(
            performAction("customswaivers" -> inputValue.toString),
            nextCall
          )
        }

        "Yes is selected" in {
          testRedirection(true, routes.EligibilityController.getEoriCheck().url)
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

        val previousUrl = routes.EligibilityController.getCustomsWaivers().url

        inSequence {
          mockAuthWithNoEnrolment()
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
            mockAuthWithNoEnrolment()
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
            mockAuthWithNoEnrolment()
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

    "handling request to get Not eligible" must {

      def performAction() = controller
        .getNotEligible(
          FakeRequest("GET", routes.EligibilityController.getNotEligible().url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithNoEnrolment()
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

      "throw technical error" when {

        "call to retrieve email  fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("some@test.com").some))
            )
            mockGet[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get eligibility passes but fetches nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("some@test.com").some))
            )
            mockGet[EligibilityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def testDisplay(eligibilityJourney: EligibilityJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("some@test.com").some))
            )
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney.some))
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

      "redirect to next page" when {

        "retrieved email is Unverified" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, EmailAddress("some@test.com").some))
            )
          }
          checkIsRedirect(performAction(), routes.UpdateEmailAddressController.updateUnverifiedEmailAddress().url)
        }

        "retrieved email is UnDeliverable" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.UnDeliverableEmail, EmailAddress("some@test.com").some))
            )
          }
          checkIsRedirect(performAction(), routes.UpdateEmailAddressController.updateUndeliveredEmailAddress().url)
        }

      }
    }

    "handling request to post EORI check" must {

      def performAction(data: (String, String)*) = controller
        .postEoriCheck(
          FakeRequest("POST", routes.EligibilityController.postEoriCheck().url)
            .withFormUrlEncodedBody(data: _*)
        )

      def update(ej: EligibilityJourney) = ej.copy(customsWaivers = CustomsWaiversFormPage(true.some))

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

        def testRedirection(input: Boolean, nextCall: String) =
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(
              Right(EligibilityJourney(eoriCheck = EoriCheckFormPage(input.some)))
            )

            checkIsRedirect(performAction("eoricheck" -> input.toString), nextCall)
          }

        "yes is selected" in {
          testRedirection(true, routes.EligibilityController.getMainBusinessCheck().url)
        }

        "No is selected" in {
          testRedirection(false, routes.EligibilityController.getIncorrectEori().url)
        }
      }

    }

    "handling request to get not eligible to lead" must {

      def performAction() = controller
        .getNotEligibleToLead(
          FakeRequest("GET", routes.EligibilityController.getNotEligibleToLead().url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("notEligibleToLead.title")
        )

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
            mockGet[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
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

        val previousUrl = routes.EligibilityController.getEoriCheck().url

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
                    doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
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
          testDisplay(
            EligibilityJourney(
              eoriCheck = EoriCheckFormPage(true.some),
              mainBusinessCheck = MainBusinessCheckFormPage(true.some)
            )
          )
        }

        "user has already answered the question" in {
          List(true, false).foreach { inputValue =>
            withClue(s" For input value :: $inputValue") {
              testDisplay(
                EligibilityJourney(
                  eoriCheck = EoriCheckFormPage(true.some),
                  mainBusinessCheck = MainBusinessCheckFormPage(inputValue.some)
                )
              )
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
        customsWaivers = CustomsWaiversFormPage(true.some)
      )
      def updatedEligibilityJourney(input: Boolean) =
        eligibilityJourney.copy(mainBusinessCheck = MainBusinessCheckFormPage(input.some))

      def update(ej: EligibilityJourney) =
        ej.copy(mainBusinessCheck = ej.mainBusinessCheck.copy(value = true.some))

      "throw technical error" when {

        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get update eligibility journey fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious(eori1)(Right(previousUrl))
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("mainbusinesscheck" -> "true")))
        }

      }

      "show form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori1)(Right(previousUrl))
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("mainbusinesscheck.title"),
            messageFromMessageKey("mainbusinesscheck.error.required")
          )
        }
      }

      "redirect to next page" when {

        def testRedirection(input: Boolean, nextCall: String) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious(eori1)(Right(previousUrl))
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(
              Right(updatedEligibilityJourney(input))
            )
          }
          checkIsRedirect(performAction("mainbusinesscheck" -> input.toString), nextCall)
        }

        "Yes is selected" in {
          testRedirection(true, routes.EligibilityController.getTerms().url)
        }

        "No is selected" in {
          testRedirection(false, routes.EligibilityController.getNotEligibleToLead().url)
        }

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
            mockGetPrevious[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
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

      def update(ej: EligibilityJourney) = ej.copy(acceptTerms = ej.acceptTerms.copy(value = true.some))

      "throw technical error" when {

        "Form value is missing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update eligibility journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("terms" -> "true")))
        }
      }

      "redirect to next page" in {
        val expectedAuditEvent = TermsAndConditionsAccepted(eori1)
        val updatedJourney = eligibilityJourney.copy(acceptTerms = AcceptTermsFormPage(true.some))
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(Right(updatedJourney))
          mockSendAuditEvent(expectedAuditEvent)
        }
        checkIsRedirect(performAction("terms" -> "true"), routes.EligibilityController.getCreateUndertaking().url)
      }

    }

    "handling request to getIncorrectEori" must {
      def performAction() = controller
        .getIncorrectEori(
          FakeRequest("GET", routes.EligibilityController.getIncorrectEori().url)
        )

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
        }
        checkPageIsDisplayed(performAction(), messageFromMessageKey("incorrectEori.title"))
      }
    }

    "handling request to getCreateUndertaking" must {
      def performAction() = controller
        .getCreateUndertaking(
          FakeRequest("GET", routes.EligibilityController.getCreateUndertaking().url)
        )

      "throw technical error" when {

        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" in {

        val previousUrl = routes.EligibilityController.getTerms().url
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGetPrevious[EligibilityJourney](eori1)(Right(routes.EligibilityController.getTerms().url))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("createUndertaking.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe previousUrl

            val button = doc.select("form")
            button.attr("action") shouldBe routes.EligibilityController.postCreateUndertaking().url

          }
        )
      }

    }

    "handling request to postCreateUndertaking" must {
      def performAction(data: (String, String)*) = controller
        .postCreateUndertaking(
          FakeRequest("POST", routes.EligibilityController.postCreateUndertaking().url)
            .withFormUrlEncodedBody(data: _*)
        )

      def update(ej: EligibilityJourney) = ej.copy(createUndertaking = CreateUndertakingFormPage(true.some))

      "throw technical error" when {

        "Form value is missing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update eligibility journey failed" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(
              Left(ConnectorError(exception))
            )
          }
          assertThrows[Exception](await(performAction("createUndertaking" -> "true")))
        }

      }

      "redirect to next page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockUpdate[EligibilityJourney](_ => update(eligibilityJourney), eori1)(
            Right(EligibilityJourney(createUndertaking = CreateUndertakingFormPage(true.some)))
          )
        }
        checkIsRedirect(
          performAction(("createUndertaking" -> "true")),
          routes.UndertakingController
            .getUndertakingName()
            .url
        )

      }

    }
  }

}
