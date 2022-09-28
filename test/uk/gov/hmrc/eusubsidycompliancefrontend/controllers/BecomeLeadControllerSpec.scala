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
import com.mongodb.client.result.UpdateResult
import com.typesafe.config.ConfigFactory
import org.bson.BsonBoolean
import play.api.Configuration
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress, EmailVerificationResponse, VerifiedEmail}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.FormPages.{AcceptResponsibilitiesFormPage, BecomeLeadEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.mongo.cache.CacheItem

import java.time.Instant
import scala.concurrent.Future

class BecomeLeadControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with EmailVerificationSupport
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

  "BecomeLeadControllerSpec" when {

    "handling request to get Become Lead Eori" must {

      def performAction() = controller.getBecomeLeadEori(FakeRequest())
      behave like authBehaviourWithPredicate(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking returns None" in {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(None))
            mockRetrieveUndertaking(eori4)(None.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        "Become lead journey is blank " in {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(None))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockPut[BecomeLeadJourney](newBecomeLeadJourney, eori4)(Right(newBecomeLeadJourney))
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
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(
              Right(
                newBecomeLeadJourney
                  .copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
                  .some
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
        .postBecomeLeadEori(FakeRequest().withFormUrlEncodedBody(data: _*))

      "throw technical error" when {

        "call to get become lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            mockGet[BecomeLeadJourney](eori4)(Left(ConnectorError("Error")))
          }
          assertThrows[Exception](await(performAction("becomeAdmin" -> "true")))
        }

      }

      "Display form error" when {

        "Nothing is submitted" in {

          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title"),
            messageFromMessageKey("becomeAdmin.error.required")
          )
        }

        "An invalid form is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
          }
          checkFormErrorIsDisplayed(
            performAction("invalidFieldName" -> "true"),
            messageFromMessageKey("become-admin.title"),
            messageFromMessageKey("becomeAdmin.error.required")
          )

        }

      }

      // TODO - remove testRedirection function
      "Redirect to next page" when {

        "user submits Yes" in {

          def testRedirection(input: String, nextCall: String) = {
            inSequence {
              mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
              mockGet[BecomeLeadJourney](eori4)(
                Right(newBecomeLeadJourney.copy(acceptResponsibilities = AcceptResponsibilitiesFormPage(true.some)).some)
              )
              mockGetUndertaking(eori4)(undertaking1.toFuture)
              mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
              mockSendEmail(eori4, PromotedSelfToNewLead, undertaking1)(Right(EmailSent))
              mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
              mockSendEmail(eori1, RemovedAsLeadToFormerLead, undertaking1)(Right(EmailSent))
              mockDelete[UndertakingJourney](eori4)(Right(()))
              mockDelete[BecomeLeadJourney](eori4)(Right(()))
              mockSendAuditEvent[BusinessEntityPromotedSelf](
                AuditEvent.BusinessEntityPromotedSelf(undertakingRef, "1123", eori1, eori4)
              )
            }

            checkIsRedirect(performAction("becomeAdmin" -> input), nextCall)
          }

          testRedirection("true", routes.BecomeLeadController.getPromotionConfirmation().url)
        }

        "user submits No" in {
          def testRedirection(input: String, nextCall: String) = {
            inSequence {
              mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
            }

            checkIsRedirect(performAction("becomeAdmin" -> input), nextCall)
          }

          testRedirection("false", routes.AccountController.getAccountPage().url)
        }

        }
      }

    }

    // TODO - reorder the test, this is the first page in the journey
    "handling request to get Accept Responsibilities" must {

      def performAction() = controller.getAcceptResponsibilities(FakeRequest())
      behave like authBehaviourWithPredicate(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to get undertaking fails" in {
          inSequence {
            mockAuthWithEccEnrolmentOnly(eori4)
            mockGetUndertaking(eori4)(Future.failed(new IllegalStateException()))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch new become lead journey fails" in {
          inSequence {
            mockAuthWithEccEnrolmentOnly(eori4)
            mockGetUndertaking(eori4)(undertaking.toFuture)
            mockGetOrCreate(eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page where the become lead journey exists" in {

        inSequence {
          mockAuthWithEccEnrolmentOnly(eori4)
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
          messageFromMessageKey("become-admin-responsibilities.title")
        )

      }

      "display the page where the become lead journey does not exist" in {

        inSequence {
          mockAuthWithEccEnrolmentOnly(eori4)
          mockGetUndertaking(eori4)(undertaking.toFuture)
          mockGetOrCreate[BecomeLeadJourney](eori4)(Right(BecomeLeadJourney()))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("become-admin-responsibilities.title")
        )

      }

    }

    "handling request to post accept responsibilities" must {

      def performAction() = controller.postAcceptResponsibilities(FakeRequest())

      "redirect" when {

        "redirect to email confirmation page if no verified email exists" in {
          def update(j: BecomeLeadJourney) = j.copy(becomeLeadEori = BecomeLeadEoriFormPage())

          inSequence {
            mockAuthWithEccEnrolmentOnly(eori4)
            mockUpdate[BecomeLeadJourney](_ => update(BecomeLeadJourney()), eori4)(Right(newBecomeLeadJourney))
          }
          redirectLocation(performAction()) shouldBe routes.BecomeLeadController.getConfirmEmail().url.some
        }
      }
    }

    "handling request to get Promotion Confirmation" must {

      def performAction() = controller.getPromotionConfirmation(FakeRequest())

      "display the page" when {

        def testDisplay(): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolmentWithValidEmail(eori4)
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin-confirmation.title")
          )
        }

        "request is successful" in {
          testDisplay()
        }

      }

    }

  "handling request to get Confirm Email page" must {

    def performAction() = controller.getConfirmEmail(FakeRequest())

    "display the correct page" when {

      "user has a verified email address in our store" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerification()
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("confirmEmail.title"))
      }

      "user has no verified email address but they do have an email address in CDS" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerification(Option.empty)
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("foo@example.com").some)))
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("confirmEmail.title"))
        contentAsString(result) should include("foo@example.com")
      }

      "user has no verified email nor do they have an email address in CDS" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockGetEmailVerification(Option.empty)
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
        }

        val result = performAction()

        status(result) shouldBe OK
        contentAsString(result) should include(messages("inputEmail.title"))

      }
    }

  }

  // TODO - check error handling here - looks like we have a missing message key
  "handling request to post Confirm Email page" must {

    val verificationUrl = routes.BecomeLeadController.getVerifyEmail("SomeId").url

    def performAction(data: (String, String)*) =
      controller.postConfirmEmail(FakeRequest().withFormUrlEncodedBody(data: _*))

    "redirect to the correct page" when {

      "user selects existing verified email address" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGetEmailVerification()
          mockAddVerifiedEmail(eori1, "foo@example.com")(Future.successful())
          mockUpdate[UndertakingJourney](identity, eori1)(Right(undertakingJourneyComplete))
        }

        val result = performAction("using-stored-email" -> "true")

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getBecomeLeadEori().url)
      }

      "user has existing verified email address but elects to enter a new one" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGetEmailVerification()
          mockMakeVerificationRequestAndRedirect(Redirect(verificationUrl).toFuture)
        }

        val result = performAction(
          "using-stored-email" -> "false",
          "email" -> "foo@example.com"
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(verificationUrl)

      }

      "user has no existing email address and enters a new one" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGetEmailVerification(None)
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
          mockMakeVerificationRequestAndRedirect(Redirect(verificationUrl).toFuture)
        }

        val result = performAction(
          "using-stored-email" -> "false",
          "email" -> "foo@example.com"
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(verificationUrl)
      }
    }

  }

  "handling request to get Verify Email page" must {

    val verificationId = "SomeVerificationId"

    def performAction(verificationId: String) = controller.getVerifyEmail(verificationId)(FakeRequest())

    "redirect to the correct page" when {

      "the become lead journey is not found" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(None))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage().url)
      }

      "the email verification record is not found" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          mockApproveVerification(eori1, verificationId)(Right(UpdateResult.acknowledged(1, 1, BsonBoolean.TRUE)))
          mockGetEmailVerification(Option.empty)
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage().url)
      }

      "the verification request is successful" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          // TODO - get this into a fixture - also review API - this is leaking internal implementation
          mockApproveVerification(eori1, verificationId)(Right(UpdateResult.acknowledged(1, 1, BsonBoolean.TRUE)))
          mockGetEmailVerification()
          mockUpdate[UndertakingJourney](identity, eori1)(Right(undertakingJourneyComplete))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getBecomeLeadEori().url)
      }

      "the verification request is not successful" in {
        inSequence {
          mockAuthWithEccEnrolmentOnly(eori1)
          mockGet[BecomeLeadJourney](eori1)(Right(newBecomeLeadJourney.some))
          // TODO - get this into a fixture - also review API - this is leaking internal implementation
          mockApproveVerification(eori1, verificationId)(Right(UpdateResult.acknowledged(0, 1, BsonBoolean.TRUE)))
        }

        val result = performAction(verificationId)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.BecomeLeadController.getConfirmEmail().url)
      }
    }

  }

}
