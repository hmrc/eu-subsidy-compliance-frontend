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
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney.Forms.NilReturnFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, NilReturnJourney, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, NonHmrcSubsidy, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.LocalDate
import scala.concurrent.Future

class AccountControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with TimeProviderSupport
    with EscServiceSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(authSupport.mockAuthConnector),
    bind[EmailVerificationService].toInstance(authSupport.mockEmailVerificationService),
    bind[Store].toInstance(journeyStoreSupport.mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[EmailService].toInstance(mockEmailService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration.from(
      Map(
        // Disable CSP n=once hashes in rendered output
        "play.filters.csp.nonce.enabled" -> false
      )
    )
  )

  private val controller = instanceOf[AccountController]

  "AccountController" when {

    "handling request to get Account page" must {

      def performAction() = controller.getAccountPage(FakeRequest())

      behave like authAndSessionDataBehaviour.authBehaviour(() => performAction())

      "display the lead account home page" when {

        def test(undertaking: Undertaking): Unit = {

          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            authAndSessionDataBehaviour. mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveAllSubsidies(undertakingRef)(undertakingSubsidies.toFuture)
            mockTimeProviderToday(fixedDate)
            journeyStoreSupport.mockGetOrCreate(eori1)(Right(nilJourneyCreate))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title"),
            { doc =>
              val htmlBody = doc.toString

              val elementIds = List(
                (1, 1),
                (1, 2),
                (2, 1),
                (3, 1),
                (4, 1),
                (4, 2),
                (4, 3)
              )

              elementIds foreach { elementId =>
                val messageKey = s"lead-account-homepage.ul${elementId._1}-li${elementId._2}"

                withClue(s"Could not locate content for messageKey: '$messageKey' in raw page content") {
                  htmlBody.contains(messageFromMessageKey(messageKey)) shouldBe true
                }
              }

            }
          )
        }

        def testTimeToReport(
          undertaking: Undertaking,
          currentDate: LocalDate,
          dueDate: String,
          isOverdue: Boolean
        ): Unit = {
          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))

            // If we have a lastSubsidyUsageUpdt on the undertaking, return a single subsidy in the mock response with
            // this date, otherwise return no subsidies.
            val subsidies =
              undertaking.lastSubsidyUsageUpdt
                .fold(List.empty[NonHmrcSubsidy])(d => List(nonHmrcSubsidy.copy(submissionDate = d)))

            mockRetrieveAllSubsidies(undertakingRef)(
              undertakingSubsidies.copy(nonHMRCSubsidyUsage = subsidies).toFuture
            )

            mockTimeProviderToday(currentDate)
            journeyStoreSupport.mockGetOrCreate[NilReturnJourney](eori1)(Right(nilJourneyCreate))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title")
          )
        }

        "there is a view link on the page and undertaking has lead only business entity" in {
          test(undertaking)
        }

        "there is an add link on the page" in {
          test(undertaking.copy(undertakingBusinessEntity = List(businessEntity1)))
        }

        "The undertaking has at least one non-Lead business entity" in {
          test(undertaking1)
        }

        "today's date falls before the next deadline" in {
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = LocalDate.of(2021, 12, 1).some),
            currentDate = LocalDate.of(2022, 2, 16),
            dueDate = "1 March 2022",
            isOverdue = false
          )
        }

        "today's date is after the deadline" in {
          val lastUpdatedDate = LocalDate.of(2021, 12, 1)
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = lastUpdatedDate.some),
            currentDate = lastUpdatedDate.plusDays(91),
            dueDate = "1 March 2022",
            isOverdue = true
          )
        }

        "due date falls back to current date plus 90 days where no lastSubsidyUsageUpdt value set on undertaking" in {
          val currentDate = LocalDate.of(2022, 1, 1)

          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = None),
            currentDate = currentDate,
            dueDate = "1 April 2022", // currentDate + 90 days
            isOverdue = false
          )

        }

      }

      "display the non-lead account home page" when {

        "valid request for non-lead user is made" when {

          "Only ECC enrolment is present" in {
            inSequence {
              authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification(eori4)
              mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
              journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori4)(Right(eligibilityJourneyComplete))
              journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori4)(Right(UndertakingJourney()))
              mockRetrieveAllSubsidies(undertakingRef)(undertakingSubsidies.toFuture)
              mockTimeProviderToday(fixedDate)
            }

            checkPageIsDisplayed(
              performAction(),
              messageFromMessageKey("non-lead-account-homepage.title"),
              { doc =>
                val htmlBody = doc.select(".govuk-list").html
                htmlBody should include regex routes.BecomeLeadController.getAcceptResponsibilities().url
                htmlBody should include regex routes.FinancialDashboardController.getFinancialDashboard.url
                htmlBody should include regex routes.BusinessEntityController.getRemoveYourselfBusinessEntity.url
              }
            )
          }

        }
      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching eligibility journey data" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in retrieving undertaking journey data" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete))
            journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching Business entity journey data" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "redirect to next page" when {

        "Only ECC enrolment" when {

          "retrieve undertaking journey is not there" in {
            inSequence {
              authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification(eori1)
              mockRetrieveUndertaking(eori1)(None.toFuture)
              journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(EligibilityJourney()))
              journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage.url)
          }

        }

        "Both CDS nd ECC enrolment present and there is no existing retrieve undertaking" when {

          "eligibility Journey is not complete and undertaking Journey is blank" in {
            inSequence {
              authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete))
              journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage)
          }

          "eligibility Journey  is complete and undertaking Journey is not complete" in {
            inSequence {
              authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.UndertakingController.firstEmptyPage)
          }

          "eligibility Journey  and undertaking Journey are  complete" in {
            inSequence {
              authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              journeyStoreSupport.mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              journeyStoreSupport.mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete1))
            }
            checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity)
          }
        }

      }

    }

  }

}
