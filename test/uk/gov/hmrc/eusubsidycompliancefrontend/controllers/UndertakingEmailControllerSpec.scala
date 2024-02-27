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
class UndertakingEmailControllerSpec
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

  private val controller = instanceOf[UndertakingEmailController]
  val verificationUrlNew = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.New).url
  val verificationUrlAmend = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.Amend).url
  val exception = new Exception("oh no")

  "UndertakingEmailController" when {
    "handling request to get verify email" must {

      def performAction(pendingVerificationId: String) = controller.getVerifyEmail(pendingVerificationId)(
        FakeRequest(GET, routes.UndertakingEmailController.getVerifyEmail(pendingVerificationId).url)
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("abcdefh")))
        }

      }

      "display the page" when {

        "User has verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val email = "joebloggs@something.com"

          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerificationStatus(
              Future.successful(Some(VerificationStatus(emailAddress = email, verified = true, locked = false)))
            )
            mockUpdateEmailForEori(eori1, email)(Future.successful(()))
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourney))
          }

          redirectLocation(performAction("id")) shouldBe Some(
            routes.UndertakingAddBusinessController.getAddBusiness.url
          )
        }
      }

    }

    "handling request to get confirm email" must {

      def performAction() =
        controller.getConfirmEmail(FakeRequest(GET, routes.UndertakingEmailController.getConfirmEmail.url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "User has verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some)
          )
          val previousCall = routes.UndertakingSectorController.getSector.url

          val email = "email@test.com"
          val pageTitle = s"Is $email the right email address to receive notifications?"
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
            )
          }
          checkPageIsDisplayed(
            performAction(),
            pageTitle,
            { doc =>
              val heading = doc.getElementsByClass("govuk-fieldset__heading")
              heading.size shouldBe 1
              heading.text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              doc
                .getElementsByTag("legend")
                .hasClass(
                  "govuk-fieldset__legend govuk-fieldset__legend--xl govuk-!-display-block break-word"
                ) shouldBe true

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController.postConfirmEmail.url
            }
          )

        }

        "User does not have verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some)
          )
          val previousCall = routes.UndertakingSectorController.getSector.url
          val email = "email@t.com"

          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
            )
          }
          checkPageIsDisplayed(
            performAction(),
            "Is email@t.com the right email address to receive notifications?",
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController.postConfirmEmail.url
            }
          )

        }
      }

      "redirect to about undertaking page" when {

        "call to fetch undertaking journey returns no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.AboutUndertakingController.getAboutUndertaking.url)
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

    "handling request to post confirm email call" must {

      def performAction(data: (String, String)*) =
        controller.postConfirmEmail(
          FakeRequest(POST, routes.UndertakingEmailController.postConfirmEmail.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        "email submitted is empty" in {

          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
          }
          assertThrows[Exception](
            await(
              performAction(
                "using-stored-email" -> "false"
              )
            )
          )
        }

        "email submitted is invalid" in {

          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
          }
          assertThrows[Exception](
            await(
              performAction(
                "using-stored-email" -> "false",
                "email" -> "joe bloggs"
              )
            )
          )
        }

      }

      "redirect to add business page" when {

        "all api calls are successful" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyWithCyaNotVisited.some))
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
            )
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))

          }
          checkIsRedirect(
            performAction("using-stored-email" -> "true"),
            routes.UndertakingAddBusinessController.getAddBusiness.url
          )
        }

        "No verification found or cds with valid form should redirect" in {
          inSequence {

            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage()).some)
            )
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("email").some))
            )
          }
          val result = performAction(
            "using-stored-email" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(verificationUrlNew)
        }

      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(
            performAction("using-stored-email" -> "false", "email" -> "somethingl.com"),
            routes.RegistrationSubmittedController.registrationAlreadySubmitted.url
          )
        }
      }
    }

    "handling request to get add email for verification" must {

      def performAction(status: EmailStatus = EmailStatus.New) =
        controller.getAddEmailForVerification(status)(FakeRequest(GET, "/some-url"))

      "display the page" when {

        "status is 'new' (user is in registration journey)" in {

          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe routes.UndertakingSectorController.getSector.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController
                .postAddEmailForVerification(EmailStatus.New)
                .url
            }
          )

        }

        "status is 'unverified' (user has no verified email address)" in {
          val status = EmailStatus.Unverified
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe routes.UnverifiedEmailController.unverifiedEmail.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController.postAddEmailForVerification(status).url
            }
          )

        }

        "status is 'amend' (user clicks 'amend email')" in {
          val status = EmailStatus.Amend
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.UndertakingAmendDetailsController.getAmendUndertakingDetails.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController
                .postAddEmailForVerification(EmailStatus.Amend)
                .url
            }
          )

        }

        "status is 'BecomeLead' (user has come from 'BecomeLead' controller)" in {
          val status = EmailStatus.BecomeLead
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.BecomeLeadController.getConfirmEmail.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController
                .postAddEmailForVerification(EmailStatus.BecomeLead)
                .url
            }
          )

        }
        "status is 'CYA' (user has come from 'change' link in CYA page)" in {
          val status = EmailStatus.CYA
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.UndertakingCheckYourAnswersController.getCheckAnswers.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingEmailController
                .postAddEmailForVerification(EmailStatus.CYA)
                .url
            }
          )

        }

      }

    }

    "handling request to post add email for verification" must {

      def performAction(status: EmailStatus, data: (String, String)*) =
        controller.postAddEmailForVerification(status = status)(
          FakeRequest(POST, "/some-url").withFormUrlEncodedBody(data: _*)
        )

      "redirect to home page when status is EmailStatus.Unverified" in {
        val redirectUrl = routes.AccountController.getAccountPage.url
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockMakeVerificationRequestAndRedirect(Redirect(redirectUrl).toFuture)
        }
        checkIsRedirect(
          performAction(status = EmailStatus.Unverified, data = ("email" -> "foo@example.com")),
          redirectUrl
        )
      }

      "redirect to add business page when status is EmailStatus.New" in {
        val redirectUrl = routes.UndertakingAddBusinessController.getAddBusiness.url
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockMakeVerificationRequestAndRedirect(Redirect(redirectUrl).toFuture)
        }
        checkIsRedirect(performAction(status = EmailStatus.New, data = ("email" -> "foo@example.com")), redirectUrl)
      }

    }

  }
}
