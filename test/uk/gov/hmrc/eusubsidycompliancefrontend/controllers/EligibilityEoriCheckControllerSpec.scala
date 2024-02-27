/*
 * Copyright 2024 HM Revenue & Customs
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
import org.jsoup.Jsoup
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, await, contentAsString, redirectLocation, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms.{DoYouClaimFormPage, EoriCheckFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailVerificationService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eligibilityJourney, eori1, undertaking}
import play.api.test.Helpers._

class EligibilityEoriCheckControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EscServiceSupport {
  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  private val controller = instanceOf[EligibilityEoriCheckController]

  private val exception = new Exception("oh no!")

  "EligibilityEoriCheckController" when {
    "handling request to get EORI check" must {

      def performAction() = controller
        .getEoriCheck(
          FakeRequest(GET, routes.EligibilityEoriCheckController.getEoriCheck.url)
            .withFormUrlEncodedBody()
        )

      "display the page" when {
        def testDisplay(eligibilityJourney: EligibilityJourney): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("eoricheck.title"),
            { doc =>
              doc
                .getElementById("eoricheck-desc-1")
                .text shouldBe "This is the EORI number that is registered to your Government Gateway ID."
              val legend = doc.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m")
              legend.size shouldBe 1
              legend.text shouldBe s"Is the EORI number you want to register $eori1?"

              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.eoriCheck.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true

              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityEoriCheckController.postEoriCheck.url
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

        "XIEORI content is available " in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }

          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          document.getElementById("page-heading").text() shouldBe "Registering an EORI number"
          document
            .getElementById("eoricheck-desc-1")
            .text() shouldBe "This is the EORI number that is registered to your Government Gateway ID."
          document.getElementById("paragraphId").text() shouldBe
            "This is the same as, and linked with any XI EORI number you may also have. That means that if you have GB123456123456, the XI version of it would be XI123456123456."
          document
            .getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m")
            .text shouldBe s"Is the EORI number you want to register $eori1?"
          document.select("form").attr("action") shouldBe routes.EligibilityEoriCheckController.postEoriCheck.url
        }
      }

      "redirect to account home" when {
        "undertaking has already been created" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          val result = performAction()
          redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }

    "handling request to post EORI check" must {
      def performAction(data: (String, String)*) = controller
        .postEoriCheck(
          FakeRequest(POST, routes.EligibilityEoriCheckController.postEoriCheck.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        "eligibility journey fail to update" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockUpdate[EligibilityJourney](eori1)(
              Left(ConnectorError(exception))
            )
          }
          assertThrows[Exception](await(performAction("eoricheck" -> "true")))
        }
      }

      "display form error" when {
        "nothing is submitted" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
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
          doYouClaim = DoYouClaimFormPage(true.some)
        )

        def testRedirection(input: Boolean, nextCall: String): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockUpdate[EligibilityJourney](eori1)(
              Right(journey.copy(eoriCheck = EoriCheckFormPage(input.some)))
            )
            checkIsRedirect(performAction("eoricheck" -> input.toString), nextCall)
          }
        }

        "yes is selected" in {
          testRedirection(input = true, routes.UndertakingController.getAboutUndertaking.url)
        }

        "no is selected" in {
          testRedirection(input = false, routes.EligibilityEoriCheckController.getIncorrectEori.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }
    }

    "handling request to get incorrect eori" must {
      def performAction() = controller
        .getIncorrectEori(
          FakeRequest(GET, routes.EligibilityEoriCheckController.getIncorrectEori.url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("incorrectEori.title")
        )
      }
    }
  }
}
