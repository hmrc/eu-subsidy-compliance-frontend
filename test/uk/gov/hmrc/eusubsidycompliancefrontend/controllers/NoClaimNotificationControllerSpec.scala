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
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney.Forms.NilReturnFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, NilSubmissionDate, SubsidyUpdate}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps

import java.time.LocalDate

class NoClaimNotificationControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(authSupport.mockAuthConnector),
    bind[Store].toInstance(journeyStoreSupport.mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailVerificationService].toInstance(authSupport.mockEmailVerificationService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )
  private val controller = instanceOf[NoClaimNotificationController]

  "NoClaimNotificationControllerSpec" when {

    "handling request to get No claim notification" must {

      def performAction = controller.getNoClaimNotification(FakeRequest())
      behave like authAndSessionDataBehaviour.authBehaviour(() => performAction)

      val startDate = LocalDate.of(2018, 4, 6)

      "display the page correctly if no previous claims have been submitted" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(fixedDate)
          mockRetrieveSubsidiesForDateRange(undertakingRef, (startDate, fixedDate))(emptyUndertakingSubsidies.toFuture)
          mockTimeProviderToday(fixedDate)
        }

        checkPageIsDisplayed(
          performAction,
          messageFromMessageKey("noClaimNotification.never-submitted.title", startDate.toDisplayFormat),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.SubsidyController.getReportPaymentFirstTimeUser.url
            val selectedOptions = doc.select(".govuk-checkboxes__input[checked]")
            selectedOptions.isEmpty shouldBe true

            val button = doc.select("form")
            button.attr("action") shouldBe routes.NoClaimNotificationController.postNoClaimNotification.url
          }
        )

      }

      "display the page correctly if at least one previous claim has been submitted" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(fixedDate)
          mockRetrieveSubsidiesForDateRange(undertakingRef, (startDate, fixedDate))(undertakingSubsidies.toFuture)
          mockTimeProviderToday(fixedDate)
        }

        checkPageIsDisplayed(
          performAction,
          messageFromMessageKey("noClaimNotification.has-submitted.title", fixedDate.toDisplayFormat),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.SubsidyController.getReportedPaymentReturningUserPage.url
            val selectedOptions = doc.select(".govuk-checkboxes__input[checked]")
            selectedOptions.isEmpty shouldBe true

            val button = doc.select("form")
            button.attr("action") shouldBe routes.NoClaimNotificationController.postNoClaimNotification.url
          }
        )

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }

    }

    "handling request to post No claim notification " must {

      val updatedNilReturnJourney = NilReturnJourney(NilReturnFormPage(true.some), displayNotification = true)

      def performAction(data: (String, String)*) = controller
        .postNoClaimNotification(
          FakeRequest(POST, "/")
            .withFormUrlEncodedBody(data: _*)
        )

      val currentDay = LocalDate.of(2022, 10, 9)

      val dateRange = (LocalDate.of(2020, 4, 6), currentDay)

      "throw technical error" when {

        val updatedNilReturnJourney = NilReturnJourney(NilReturnFormPage(true.some))

        val exception = new Exception("oh no")

        "call to update  Nil return journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDay)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies.toFuture)
            mockTimeProviderToday(currentDay)
            mockTimeProviderToday(currentDay)
            journeyStoreSupport.mockUpdate[NilReturnJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))
        }

        "call to create Subsidy fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDay)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies.toFuture)
            mockTimeProviderToday(currentDay)
            mockTimeProviderToday(currentDay)
            journeyStoreSupport.mockUpdate[NilReturnJourney](eori1)(Right(updatedNilReturnJourney))
            mockCreateSubsidy(SubsidyUpdate(undertakingRef, NilSubmissionDate(currentDay.plusDays(1))))(
              Left(ConnectorError(exception))
            )
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))
        }

      }

      "show form error" when {

        "check box is not checked" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDay)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies.toFuture)
            mockTimeProviderToday(currentDay)
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("noClaimNotification.has-submitted.title", "20 January 2021"),
            messageFromMessageKey("noClaimNotification.error.required")
          )
        }
      }

      "redirect to next page " in {

        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(currentDay)
          mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies.toFuture)
          mockTimeProviderToday(currentDay)
          mockTimeProviderToday(currentDay)
          journeyStoreSupport.mockUpdate[NilReturnJourney](eori1)(Right(updatedNilReturnJourney))
          mockCreateSubsidy(SubsidyUpdate(undertakingRef, NilSubmissionDate(currentDay.plusDays(1))))(
            Right(undertakingRef)
          )
          mockSendAuditEvent(
            AuditEvent.NonCustomsSubsidyNilReturn("1123", eori1, undertakingRef, currentDay.plusDays(1))
          )
        }
        checkIsRedirect(
          performAction("noClaimNotification" -> "true"),
          routes.NoClaimNotificationController.getNotificationConfirmation.url
        )
      }
    }
  }

}
