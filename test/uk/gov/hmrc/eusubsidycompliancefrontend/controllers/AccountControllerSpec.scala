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
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney.Forms.NilReturnFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, NilReturnJourney, UndertakingJourney}
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
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
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

      behave like authBehaviour(() => performAction())

      "display the lead account home page" when {

        def test(undertaking: Undertaking): Unit = {

          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveSubsidy(subsidyRetrieve.copy(inDateRange = None))(undertakingSubsidies.toFuture)
            mockTimeToday(fixedDate)
            mockTimeToday(fixedDate)
            mockGetOrCreate(eori1)(Right(nilJourneyCreate))
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
                (2, 2),
                (3, 1),
                (3, 2),
                (3, 3),
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
          isTimeToReport: Boolean,
          dueDate: String,
          isOverdue: Boolean
        ): Unit = {
          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveSubsidy(subsidyRetrieve.copy(inDateRange = None))(undertakingSubsidies.copy(nonHMRCSubsidyUsage = List(nonHmrcSubsidy.copy(submissionDate = undertaking.lastSubsidyUsageUpdt.get))).toFuture)
            mockTimeToday(currentDate)
            mockTimeToday(currentDate)
            mockGetOrCreate[NilReturnJourney](eori1)(Right(nilJourneyCreate))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title"),
            doc => {
              if (isTimeToReport) {
                val content = doc.select(".govuk-grid-column-two-thirds").toString
                content should include regex
                  messageFromMessageKey("lead-account-homepage.p2.not-overdue", dueDate)
              } else if (isOverdue) {
                val content = doc.select(".govuk-warning-text").text
                content should include regex
                  messageFromMessageKey("lead-account-homepage.p2.is-overdue", dueDate)
              }
            }
          )
        }

        "there is a view link on the page and undertaking has lead only business entity" in {
          test(undertaking)
        }

        "there is an add link on the page" in {
          test(undertaking.copy(undertakingBusinessEntity = List(businessEntity1)))
        }

        "The undertaking  any non-Lead  business entities " in {
          test(undertaking1)
        }

        "today's date falls before the next deadline" in {
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = LocalDate.of(2021, 12, 1).some),
            currentDate = LocalDate.of(2022, 2, 16),
            isTimeToReport = true,
            dueDate = "1 March 2022",
            isOverdue = false
          )
        }

        "today's date is after the deadling" in {
          val lastUpdatedDate = LocalDate.of(2021, 12, 1)
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = lastUpdatedDate.some),
            currentDate = lastUpdatedDate.plusDays(91),
            isTimeToReport = false,
            dueDate = "1 March 2022",
            isOverdue = true
          )
        }

      }

      "display the non-lead account home page" when {

        "valid request for non-lead user is made" when {

          "Only ECC enrolment is present" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification(eori4)
              mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
              mockGetOrCreate[EligibilityJourney](eori4)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori4)(Right(UndertakingJourney()))
              mockRetrieveSubsidy(subsidyRetrieve.copy(inDateRange = None))(undertakingSubsidies.toFuture)
              mockTimeToday(fixedDate)
              mockTimeToday(fixedDate)
            }

            checkPageIsDisplayed(
              performAction(),
              messageFromMessageKey("non-lead-account-homepage.title"),
              { doc =>
                val htmlBody = doc.select(".govuk-list").html
                htmlBody should include regex routes.BecomeLeadController.getAcceptResponsibilities().url
                htmlBody should include regex routes.FinancialDashboardController.getFinancialDashboard().url
                htmlBody should include regex routes.BusinessEntityController.getRemoveYourselfBusinessEntity().url
              }
            )
          }

        }
      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching eligibility journey data" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in retrieving undertaking journey data" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching Business entity journey data" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "redirect to next page" when {

        "Only ECC enrolment" when {

          "retrieve undertaking journey is not there" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification(eori1)
              mockRetrieveUndertaking(eori1)(None.toFuture)
              mockGetOrCreate[EligibilityJourney](eori1)(Right(EligibilityJourney()))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage().url)
          }

        }

        "Both CDS nd ECC enrolment present and there is no existing retrieve undertaking" when {

          "eligibility Journey is not complete and undertaking Journey is blank" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage())
          }

          "eligibility Journey  is complete and undertaking Journey is not complete" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            }
            checkIsRedirect(performAction(), routes.UndertakingController.firstEmptyPage())
          }

          "eligibility Journey  and undertaking Journey are  complete" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(None.toFuture)
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete1))
            }
            checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity())
          }
        }

      }

    }

  }

}
