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
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.English
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIAndDateEmailParameter, SingleEORIAndDateEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendResult, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, AuditServiceSupport, BusinessEntityJourney, EscService, JourneyTraverseService, RetrieveEmailService, SendEmailService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.LocalDate
import scala.concurrent.Future

class SignOutControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with EmailSupport
    with TimeProviderSupport
    with AuditServiceSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[RetrieveEmailService].toInstance(mockRetrieveEmailService),
    bind[SendEmailService].toInstance(mockSendEmailService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )

  private val controller = instanceOf[SignOutController]

  private val currentDate = LocalDate.of(2021, 10, 9)

  private val effectiveRemovalDate = "10 October 2021"

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |"urls.timeOutContinue" = "http://host:123/continue"
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |     member-remove-themself-email-to-be-template-en = "template_remove_yourself_be_EN"
                                   |     member-remove-themself-email-to-be-template-cy = "template_remove_yourself_be_CY"
                                   |     member-remove-themself-email-to-lead-template-en = "template_remove_yourself_lead_EN"
                                   |     member-remove-themself-email-to-lead-template-cy = "template_remove_yourself_lead_CY"
                                   |  }
                                   |""".stripMargin)
    )
  )

  "SignOutController" when {

    "handling request to signOut From Timeout" must {

      def performAction() = controller.signOutFromTimeout(FakeRequest())

      "display the page" in {

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("timedOut.title"),
          { doc =>
            val body = doc.select(".govuk-body").text()
            body should include regex messageFromMessageKey("timedOut.p1")
            body should include regex messageFromMessageKey("timedOut.signIn", appConfig.timeOutContinue)
          }
        )

      }
    }

    "handling request to get sign out" must {

      def performAction(lang: String = English.code) = controller.signOut(
        FakeRequest("GET", routes.SignOutController.signOut().url)
          .withCookies(Cookie("PLAY_LANG", lang))
      )

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to retrieve email fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "call to retrieve Undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockRetrieveUndertaking(eori4)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to retrieve Undertaking fetches Undertaking without UndertakingRef" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockRetrieveUndertaking(eori4)(Future.successful(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to remove member fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockRetrieveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockTimeToday(currentDate)
            mockRemoveMember(CommonTestData.undertakingRef, businessEntity4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "language is other than english /welsh" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
          }
          assertThrows[Exception](await(performAction("fr")))
        }
      }

      "display the page" in {
        val emailParamBE = SingleEORIAndDateEmailParameter(
          eori4,
          undertaking.name,
          undertakingRef,
          effectiveRemovalDate,
          "removeThemselfEmailToBE"
        )
        val emailParamLead = DoubleEORIAndDateEmailParameter(
          eori1,
          eori4,
          undertaking.name,
          undertakingRef,
          effectiveRemovalDate,
          "removeThemselfEmailToLead"
        )
        inSequence {
          mockAuthWithNecessaryEnrolment(eori4)
          mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
          mockRetrieveUndertaking(eori4)(Future.successful(undertaking1.some))
          mockTimeToday(currentDate)
          mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
          mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
          mockSendEmail(validEmailAddress, emailParamBE, "template_remove_yourself_be_EN")(
            Right(EmailSendResult.EmailSent)
          )
          mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
          mockSendEmail(validEmailAddress, emailParamLead, "template_remove_yourself_lead_EN")(
            Right(EmailSendResult.EmailSent)
          )
          mockSendAuditEvent(AuditEvent.BusinessEntityRemovedSelf(undertakingRef, "1123", eori1, eori4))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("signOut.title"),
          doc => doc.select(".govuk-body").text() should include regex messageFromMessageKey("signOut.p1")
        )

      }

      "redirect to next page" when {

        "email address is unverified" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, validEmailAddress.some)))
          }
          checkIsRedirect(performAction(), routes.UpdateEmailAddressController.updateUnverifiedEmailAddress().url)
        }

        "email address is Undeliverable" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori4)
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.UnDeliverableEmail, validEmailAddress.some)))
          }
          checkIsRedirect(performAction(), routes.UpdateEmailAddressController.updateUndeliveredEmailAddress().url)
        }
      }
    }

    "handling request to no cds enrolment page " must {

      def performAction() = controller.noCdsEnrolment(FakeRequest())

      "display the page" in {

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("cdsEnrolmentMissing.title"),
          doc => doc.select(".govuk-body").html() should include regex messageFromMessageKey("cdsEnrolmentMissing.p1")
        )

      }

    }
  }
}
