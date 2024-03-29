/*
 * Copyright 2023 HM Revenue & Customs
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
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import play.api.Configuration
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BecomeLeadJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BecomeLeadJourney.FormPages.AcceptResponsibilitiesFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse, VerificationStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

import scala.concurrent.Future

class BecomeLeadControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with EmailVerificationServiceSupport
    with AuditServiceSupport
    with EscServiceSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |    promoted-themself-email-to-new-lead-template-en = "template_promoted_themself_as_lead_email_to_lead_EN"
                                   |    promoted-themself-email-to-new-lead-template-cy = "template_promoted_themself_as_lead_email_to_lead_CY"
                                   |    removed_as_lead-email-to-old-lead-template-en = "template_removed_as_lead_email_to_previous_lead_EN"
                                   |    removed_as_lead-email-to-old-lead-template-cy = "template_removed_as_lead_email_to_previous_lead_CY"
                                   |  }
                                   |""".stripMargin)
    )
  )

  private val controller = instanceOf[BecomeLeadController]
  val verificationUrlBecomeLead = routes.UndertakingController.getAddEmailForVerification(EmailStatus.BecomeLead).url

  "BecomeLeadControllerSpec" when {

    "handling request to get Become Lead Eori" must {

      def performAction() = controller.getBecomeLeadEori(FakeRequest())

      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking returns None" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
            mockRetrieveUndertaking(eori4)(None.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        "When Change of undertaking administrator title is available in" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val findTitle = document.getElementById("titleId").text()
          findTitle shouldBe "Change of undertaking administrator"
        }

        "When legend is displayed in fieldset component" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val legendText: String =
            document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m").text()
          legendText shouldBe "Do you want to become the administrator for this undertaking?"
        }

        "Become lead journey is blank " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty shouldBe true

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BecomeLeadController.postBecomeLeadEori().url
            }
          )

        }

        "new lead journey already exists" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGetOrCreate[BecomeLeadJourney](eori4)(
              Right(
                newBecomeLeadJourney.copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
              )
            )
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            { doc =>
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
        .postBecomeLeadEori(FakeRequest(POST, "/").withFormUrlEncodedBody(data: _*))

      "throw technical error" when {

        "call to get become lead journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(Left(ConnectorError("Error")))
          }
          assertThrows[Exception](await(performAction("becomeAdmin" -> "true")))
        }

      }

      "Display form error" when {

        "Nothing is submitted" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            messageFromMessageKey("becomeAdmin.error.required")
          )
        }

        "An invalid form is submitted" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
          }
          checkFormErrorIsDisplayed(
            performAction("invalidFieldName" -> "true"),
            messageFromMessageKey("become-admin.title"),
            messageFromMessageKey("becomeAdmin.error.required")
          )

        }

      }

      "Redirect to next page" when {

        "user submits Yes" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(
              Right(newBecomeLeadJourney.copy(acceptResponsibilities = AcceptResponsibilitiesFormPage(true.some)).some)
            )
            mockGetUndertaking(eori4)(undertaking1.toFuture)
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockSendEmail(eori4, PromotedSelfToNewLead, undertaking1)(Right(EmailSent))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockSendEmail(eori1, RemovedAsLeadToFormerLead, undertaking1)(Right(EmailSent))
            mockDeleteAll(eori4)(Right(()))
            mockSendAuditEvent[BusinessEntityPromotedSelf](
              AuditEvent.BusinessEntityPromotedSelf(undertakingRef, "1123", eori1, eori4)
            )
          }

          checkIsRedirect(
            performAction("becomeAdmin" -> "true"),
            routes.BecomeLeadController.getPromotionConfirmation().url
          )
        }

        "user submits No" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
          }

          checkIsRedirect(performAction("becomeAdmin" -> "false"), routes.AccountController.getAccountPage.url)
        }

      }

    }

    "handling request to get Accept Responsibilities" must {

      def performAction() = controller.getAcceptResponsibilities(FakeRequest())

      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to get undertaking fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGetUndertaking(eori4)(Future.failed(new IllegalStateException()))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch new become lead journey fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGetUndertaking(eori4)(undertaking.toFuture)
            mockGetOrCreate(eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page where the become lead journey exists" in {

        inSequence {
          mockAuthWithEnrolment(eori4)
          mockGetUndertaking(eori4)(undertaking.toFuture)
          mockGetOrCreate(eori4)(
            Right(
              newBecomeLeadJourney
                .copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
            )
          )
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("become-admin-responsibilities.title"),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.AccountController.getAccountPage.url
          }
        )

      }

      "display the page where the become lead journey does not exist" in {

        inSequence {
          mockAuthWithEnrolment(eori4)
          mockGetUndertaking(eori4)(undertaking.toFuture)
          mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("become-admin-responsibilities.title"),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.AccountController.getAccountPage.url
          }
        )

      }

    }

    "handling request to post accept responsibilities" must {

      def performAction() = controller.postAcceptResponsibilities(FakeRequest())

      "redirect" when {

        "redirect to email confirmation page if no verified email exists" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockUpdate[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney))
          }
          redirectLocation(performAction()) shouldBe routes.BecomeLeadController.getConfirmEmail.url.some
        }
      }
    }

    "handling request to get Promotion Confirmation" must {

      def performAction() = controller.getPromotionConfirmation(FakeRequest())

      "display the page" when {

        def testDisplay(): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori4)
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin-confirmation.title"),
            { doc =>
              doc
                .getElementById("becomeAdminParaOne")
                .text shouldBe "We’ve sent you a confirmation email. We have also sent an email to the previous administrator."
              doc.getElementById("becomeAdminFeedbackHeader").text shouldBe "Before you go"
              doc
                .getElementById("becomeAdminParaTwo")
                .text shouldBe "Your feedback helps us make our service better."
              doc
                .getElementById("becomeAdminParaThree")
                .text shouldBe "Take our survey to share your feedback on this service. It takes about 1 minute to complete."
            }
          )
        }

        "request is successful" in {
          testDisplay()
        }

      }

    }

  }

  "handling request to get Confirm Email page" must {

    def performAction() = controller.getConfirmEmail(FakeRequest())

    "display the correct page" when {

      "user's email has already been verified in our store" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockRetrieveEmail(eori1)(
            Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("foo@example.com").some))
          )
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("confirmEmail.title", "foo@example.com"))
      }

      "user has no verified email address but they do have an email address in CDS" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockRetrieveEmail(eori1)(
            Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("foo@example.com").some))
          )
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("confirmEmail.title", "foo@example.com"))
      }

      "user has no verified email nor do they have an email address in CDS" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("inputEmail.title"))

      }
    }

  }

  "handling request to post Confirm Email page" must {

    def performAction(data: (String, String)*) =
      controller.postConfirmEmail(FakeRequest(POST, "/").withFormUrlEncodedBody(data: _*))

    "redirect to the correct page" when {

      "user selects existing verified email address" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockRetrieveEmail(eori1)(
            Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("foo@example.com").some))
          )
        }

        val result = performAction("using-stored-email" -> "true")

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getBecomeLeadEori().url)
      }

      "user has existing verified email address but elects to enter a new one" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockRetrieveEmail(eori1)(
            Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("foo@example.com").some))
          )
        }

        val result = performAction(
          "using-stored-email" -> "false"
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(verificationUrlBecomeLead)
      }

      "user has no existing email address and enters a new one" in {

        inSequence {
          mockAuthWithEnrolment(eori1)
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
        }

        val result = performAction(
          "using-stored-email" -> "false"
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(verificationUrlBecomeLead)
      }
    }

  }

  "handling request to get Verify Email page" must {

    val verificationId = "SomeVerificationId"

    def performAction(verificationId: String) = controller.getVerifyEmail(verificationId)(FakeRequest())

    "redirect to the correct page" when {

      "the become lead journey is not found" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(None))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getConfirmEmail.url)
      }

      "the email verification record is not found" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerificationStatus(Future.successful(None))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getConfirmEmail.url)
      }

      "the verification request is successful" in {
        val email = "foo@example.com"
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerificationStatus(
            Future.successful(Some(VerificationStatus(emailAddress = email, verified = true, locked = false)))
          )
          mockUpdateEmailForEori(eori1, email)(Future.successful(()))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getBecomeLeadEori().url)
      }

      "the verification request is not successful" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerificationStatus(Future.successful(None))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getConfirmEmail.url)
      }
    }

  }

}
