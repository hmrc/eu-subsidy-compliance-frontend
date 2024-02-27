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
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{CreateUndertaking, DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse, VerificationStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.{Amend, EmailStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EmailStatus, Sector, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class UndertakingAddBusinessControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with EscServiceSupport
    with EmailVerificationServiceSupport
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration.from(
      Map(
        "play.i18n.langs" -> Seq("en", "cy", "fr"),
        "email-send.create-undertaking-template-en" -> "template_EN",
        "email-send.create-undertaking-template-cy" -> "template_CY"
      )
    )
  )

  private val controller = instanceOf[UndertakingAddBusinessController]
  val verificationUrlNew = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.New).url
  val verificationUrlAmend = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.Amend).url
  val exception = new Exception("oh no")

  "UndertakingAddBusinessController" when {
    "handling request to get intention to add business" must {

      def performAction() = controller.getAddBusiness(
        FakeRequest(GET, routes.UndertakingAddBusinessController.getAddBusiness.url)
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "When Change of undertaking business is available in" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }

          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val findTitle = document.getElementById("addBusinessTitleId").text()
          findTitle shouldBe "Businesses in your undertaking"

          val legendText: String =
            document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m").text()
          legendText shouldBe "Are there other businesses in your undertaking?"

        }

        "addBusiness page has an intent" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val previousCall = routes.UndertakingEmailController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          val document = Jsoup.parse(contentAsString(performAction()))
          document.select(".govuk-back-link").attr("href") shouldBe previousCall
          document.getElementById("intentId").text() shouldBe messageFromMessageKey("addBusinessIntent.p1")
        }

        "when question has not been answered" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val previousCall = routes.UndertakingEmailController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("addBusinessIntent.title", undertakingJourney.about.value.getOrElse("")),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingAddBusinessController.postAddBusiness.url
            }
          )

        }

        "when question has been answered" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
            addBusiness = UndertakingAddBusinessFormPage(true.some)
          )
          val previousCall = routes.UndertakingEmailController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("addBusinessIntent.title", undertakingJourney.about.value.getOrElse("")),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingAddBusinessController.postAddBusiness.url
            }
          )

        }
      }

      "redirect to journey start page" when {

        "call to fetch undertaking journey passes  but return no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.AboutUndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to previous step" when {
        "email question has not been answered" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(
              Right(
                UndertakingJourney(
                  about = AboutUndertakingFormPage("TestUndertaking".some),
                  sector = UndertakingSectorFormPage(Sector(1).some)
                ).some
              )
            )
          }
          checkIsRedirect(performAction(), routes.UndertakingEmailController.getConfirmEmail.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }

    "handling request to post intention to add business" must {

      def performAction(data: (String, String)*) = controller.postAddBusiness(
        FakeRequest(POST, routes.UndertakingAddBusinessController.postAddBusiness.url).withFormUrlEncodedBody(data: _*)
      )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("addBusinessIntent" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String): Unit = {

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("addBusinessIntent.title"),
            messageFromMessageKey(errorMessage)
          )
        }

        "nothing has been submitted" in {
          displayErrorTest()("addBusinessIntent.error.required")
        }

      }

      "redirect to the next page" when {

        "user selected No" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(
              Right(UndertakingJourney(addBusiness = UndertakingAddBusinessFormPage(false.some)))
            )
          }
          checkIsRedirect(
            performAction("addBusinessIntent" -> "false"),
            routes.UndertakingCheckYourAnswersController.getCheckAnswers.url
          )
        }

        "user selected Yes" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(
              Right(UndertakingJourney(addBusiness = UndertakingAddBusinessFormPage(true.some)))
            )
          }
          checkIsRedirect(
            performAction("addBusinessIntent" -> "true"),
            routes.UndertakingCheckYourAnswersController.getCheckAnswers.url
          )
        }

      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }
  }
}
