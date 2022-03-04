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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Error, NilSubmissionDate, SubsidyUpdate, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, AuditServiceSupport, EscService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{eori1, undertaking, undertakingRef}

import java.time.LocalDate
import scala.concurrent.Future

class NoClaimNotificationControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with AuditServiceSupport {

  private val mockEscService = mock[EscService]
  private val mockTimeProvider = mock[TimeProvider]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )
  private val controller = instanceOf[NoClaimNotificationController]

  private def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  private def mockCreateSubsidy(reference: UndertakingRef, subsidyUpdate: SubsidyUpdate)(
    result: Either[Error, UndertakingRef]
  ) =
    (mockEscService
      .createSubsidy(_: UndertakingRef, _: SubsidyUpdate)(_: HeaderCarrier))
      .expects(reference, subsidyUpdate, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)), Future.successful(_)))

  private def mockTimeProviderToday(today: LocalDate) =
    (mockTimeProvider.today _).expects().returning(today)

  "NoClaimNotificationControllerSpec" when {

    "handling request to get No claim notification" must {

      def performAction() = controller.getNoClaimNotification(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to retrieve undertaking came back with None" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
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

    }

    "handling request  to post No claim notification " must {

      def performAction(data: (String, String)*) = controller
        .postNoClaimNotification(
          FakeRequest()
            .withFormUrlEncodedBody(data: _*)
        )

      val currentDay = LocalDate.of(2022, 10, 9)

      "throw technical error" when {

        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))

        }

        "call to retrieve undertaking came back with None" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))

        }

        "call to create Subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
            mockTimeProviderToday(currentDay)
            mockCreateSubsidy(undertakingRef, SubsidyUpdate(undertakingRef, NilSubmissionDate(currentDay)))(
              Left(Error(exception))
            )
          }
          assertThrows[Exception](await(performAction("noClaimNotification" -> "true")))
        }

      }

      "show form error" when {

        "check box is not checked" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
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
          mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
          mockTimeProviderToday(currentDay)
          mockCreateSubsidy(undertakingRef, SubsidyUpdate(undertakingRef, NilSubmissionDate(currentDay)))(
            Right(undertakingRef)
          )
          mockSendAuditEvent(AuditEvent.NonCustomsSubsidyNilReturn("1123", eori1, undertakingRef, currentDay))
        }
        checkIsRedirect(
          performAction("noClaimNotification" -> "true"),
          routes.NoClaimNotificationController.getNoClaimConfirmation()
        )

      }

    }

    "handling request to get No claim confirmation" must {

      def performAction() = controller.getNoClaimConfirmation(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to retrieve undertaking came back with None" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(Future.successful(undertaking.some))
        }
        checkPageIsDisplayed(
          performAction(),
          undertaking.name,
          { doc =>
            val htmlBody = doc.select(".govuk-body").html()

            htmlBody should include regex messageFromMessageKey(
              "noClaimConfirmation.link",
              routes.AccountController.getAccountPage().url
            )

          }
        )
      }

    }

  }

}
