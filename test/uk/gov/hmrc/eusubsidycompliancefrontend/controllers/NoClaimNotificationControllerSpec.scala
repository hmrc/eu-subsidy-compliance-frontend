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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, NilSubmissionDate, SubsidyUpdate}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NilReturnJourney.Forms.NilReturnFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, undertaking, undertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

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

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )
  private val controller = instanceOf[NoClaimNotificationController]

  "NoClaimNotificationControllerSpec" when {

    "handling request to get No claim notification" must {

      def performAction() = controller.getNoClaimNotification(FakeRequest())
      behave like authBehaviourWithPredicate(() => performAction())

      "display the page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("noClaimNotification.title", undertaking.name),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage().url
            val selectedOptions = doc.select(".govuk-checkboxes__input[checked]")
            selectedOptions.isEmpty shouldBe true

            val button = doc.select("form")
            button.attr("action") shouldBe routes.NoClaimNotificationController.postNoClaimNotification().url
          }
        )

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request  to post No claim notification " must {

      val nilReturnJourney = NilReturnJourney()
      val updatedNilReturnJourney = NilReturnJourney(NilReturnFormPage(true.some), true)

      def update(j: NilReturnJourney) =
        j.copy(nilReturn = j.nilReturn.copy(value = Some(true)), false)

      def performAction(data: (String, String)*) = controller
        .postNoClaimNotification(
          FakeRequest()
            .withFormUrlEncodedBody(data: _*)
        )

      val currentDay = LocalDate.of(2022, 10, 9)

      "throw technical error" when {

        val nilReturnJourney = NilReturnJourney()
        val updatedNilReturnJourney = NilReturnJourney(NilReturnFormPage(true.some), false)

        def update(j: NilReturnJourney) = j.copy(nilReturn = j.nilReturn.copy(value = Some(true)), false)

        val exception = new Exception("oh no")

        "call to update  Nil return journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDay)
            mockUpdate[NilReturnJourney](_ => update(nilReturnJourney), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))
        }

        "call to create Subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDay)
            mockUpdate[NilReturnJourney](_ => update(nilReturnJourney), eori1)(Right(updatedNilReturnJourney))
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
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("noClaimNotification.title", undertaking.name),
            messageFromMessageKey("noClaimNotification.error.required")
          )
        }
      }

      "redirect to next page " in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeToday(currentDay)
          mockUpdate[NilReturnJourney](_ => update(nilReturnJourney), eori1)(Right(updatedNilReturnJourney))
          mockCreateSubsidy(SubsidyUpdate(undertakingRef, NilSubmissionDate(currentDay.plusDays(1))))(
            Right(undertakingRef)
          )
          mockSendAuditEvent(
            AuditEvent.NonCustomsSubsidyNilReturn("1123", eori1, undertakingRef, currentDay.plusDays(1))
          )
        }
        checkIsRedirect(
          performAction("noClaimNotification" -> "true"),
          routes.AccountController.getAccountPage().url
        )
      }
    }
  }

}
