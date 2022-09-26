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
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.FormPages.{BecomeLeadEoriFormPage, AcceptResponsibilitiesFormPage}
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

          def update(j: BecomeLeadJourney) = j.copy(becomeLeadEori = BecomeLeadEoriFormPage())

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
            def update(j: BecomeLeadJourney) = j.copy(becomeLeadEori = BecomeLeadEoriFormPage())

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
            def update(j: BecomeLeadJourney) = j.copy(becomeLeadEori = BecomeLeadEoriFormPage())

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

        "call to fetch new become lead journey fails" in {
          inSequence {
            mockAuthWithEccEnrolmentOnly(eori4)
            mockGet[BecomeLeadJourney](eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" in {

        inSequence {
          mockAuthWithEccEnrolmentOnly(eori4)
          mockGet[BecomeLeadJourney](eori4)(
            Right(
              newBecomeLeadJourney
                .copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
                .some
            )
          )
          mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
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

}
