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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney.Forms.NilReturnFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, NilReturnJourney, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{Sector, UndertakingStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, NonHmrcSubsidy, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps

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
        // Disable CSP nonce hashes in rendered output
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
            mockTimeProviderToday(fixedDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
              undertakingSubsidies.toFuture
            )
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(fixedDate)
            mockGetOrCreate(eori1)(Right(nilJourneyCreate))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title"),
            { doc =>
              verifyGenericHomepageContentForLead(doc)
              doc.getElementById("lead-account-homepage-p2").text shouldBe "You must either:"

              doc.getElementById("lead-account-homepage-p1-li1").text shouldBe "registered your undertaking"
              doc
                .getElementById("lead-account-homepage-p1-li2")
                .text shouldBe "submitted your last report of receiving a non-customs subsidy payment or no payments"

              doc.getElementById("lead-account-homepage-p2-li1").text shouldBe "report a non-customs subsidy payment"
              doc
                .getElementById("lead-account-homepage-p2-li2")
                .text shouldBe "report that you have not been awarded any non-customs subsidy payments"

              verifyUndertakingBalance(doc)
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
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))

            // If we have a lastSubsidyUsageUpdt on the undertaking, return a single subsidy in the mock response with
            // this date, otherwise return no subsidies.
            val subsidies =
              undertaking.lastSubsidyUsageUpdt
                .fold(List.empty[NonHmrcSubsidy])(d => List(nonHmrcSubsidy.copy(submissionDate = d)))

            mockTimeProviderToday(currentDate)

            mockRetrieveSubsidiesForDateRange(undertakingRef, currentDate.toSearchRange)(
              undertakingSubsidies.copy(nonHMRCSubsidyUsage = subsidies).toFuture
            )
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(currentDate)
            mockGetOrCreate[NilReturnJourney](eori1)(Right(nilJourneyCreate))
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

      "redirect to regulatory change notification" when {

        "user has undertaking with agriculture sector" in {
          val agricultureUndertaking = undertaking.copy(industrySector = Sector.agriculture)

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(agricultureUndertaking.some.toFuture)
          }

          checkIsRedirect(
            performAction(),
            routes.RegulatoryChangeNotificationController.showPage
          )
        }

        "user has undertaking with other sector" in {
          val otherUndertaking = undertaking.copy(industrySector = Sector.other)

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(otherUndertaking.some.toFuture)
          }

          checkIsRedirect(
            performAction(),
            routes.RegulatoryChangeNotificationController.showPage
          )
        }

      }

      "not redirect to regulatory change notification" when {

        "user has undertaking with fishery sector" in {
          val fisheryUndertaking = undertaking.copy(industrySector = Sector.aquaculture)

          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(fisheryUndertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockTimeProviderToday(fixedDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
              undertakingSubsidies.toFuture
            )
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(fixedDate)
            mockGetOrCreate(eori1)(Right(nilJourneyCreate))
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title"),
            { doc =>
              verifyGenericHomepageContentForLead(doc)
              doc.getElementById("lead-account-homepage-p2").text shouldBe "You must either:"
            }
          )
        }

        "display account page after seeing notification" when {

          "display the page correctly for an undertaking with agriculture sector" in {
            def test(undertaking: Undertaking): Unit = {
              val requestWithSession = FakeRequest().withSession("regulatoryChangeNotificationSeen" -> "true")
              def performActionWithSession() = controller.getAccountPage(requestWithSession)

              val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
              inSequence {
                mockAuthWithEnrolmentAndNoEmailVerification()
                mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
                mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
                mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete1))
                mockTimeProviderToday(fixedDate)
                mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                  undertakingSubsidies.toFuture
                )
                mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
                mockTimeProviderToday(fixedDate)
                mockGetOrCreate(eori1)(Right(nilJourneyCreate))
              }
              checkPageIsDisplayed(
                performActionWithSession(),
                messageFromMessageKey("lead-account-homepage.title"),
                { doc =>
                  verifyGenericHomepageContentForLead(doc)
                  doc.getElementById("govuk-notification-banner-title").text should not be null
                  doc.getElementById("govuk-notification-banner-title").text shouldBe "Important"
                  doc
                    .getElementById("lead-account-homepage-details-hasSubmitted-h3-p1-agri-other")
                    .text shouldBe "You must report any payments received within your latest 90-day reporting period."
                  doc
                    .getElementById("lead-account-homepage-details-neverSubmitted-h3-p1-agri-other")
                    .text shouldBe "You must report any payments received over a rolling 3-year period, counting back from your latest declaration."

                  verifyUndertakingBalanceAgriOther(doc)
                }
              )
            }

            test(undertaking3)
          }

          "display the page correctly for an undertaking with other sector" in {
            def test(undertaking: Undertaking): Unit = {
              val requestWithSession = FakeRequest().withSession("regulatoryChangeNotificationSeen" -> "true")
              def performActionWithSession() = controller.getAccountPage(requestWithSession)

              val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
              inSequence {
                mockAuthWithEnrolmentAndNoEmailVerification()
                mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
                mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
                mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete2))
                mockTimeProviderToday(fixedDate)
                mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                  undertakingSubsidies.toFuture
                )
                mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
                mockTimeProviderToday(fixedDate)
                mockGetOrCreate(eori1)(Right(nilJourneyCreate))
              }
              checkPageIsDisplayed(
                performActionWithSession(),
                messageFromMessageKey("lead-account-homepage.title"),
                { doc =>
                  verifyGenericHomepageContentForLead(doc)
                  doc.getElementById("govuk-notification-banner-title").text should not be null
                  doc.getElementById("govuk-notification-banner-title").text shouldBe "Important"
                  doc
                    .getElementById("lead-account-homepage-details-hasSubmitted-h3-p1-agri-other")
                    .text shouldBe "You must report any payments received within your latest 90-day reporting period."
                  doc
                    .getElementById("lead-account-homepage-details-neverSubmitted-h3-p1-agri-other")
                    .text shouldBe "You must report any payments received over a rolling 3-year period, counting back from your latest declaration."

                  verifyUndertakingBalanceAgriOther(doc)
                }
              )
            }

            test(undertaking3)
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
                mockTimeProviderToday(fixedDate)
                mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                  undertakingSubsidies.toFuture
                )
                mockGetUndertakingBalance(eori4)(Future.successful(Some(undertakingBalance)))
                mockTimeProviderToday(fixedDate)
              }

              checkPageIsDisplayed(
                performAction(),
                messageFromMessageKey("non-lead-account-homepage.title"),
                { doc =>
                  val htmlBody = doc.select(".govuk-list").html
                  htmlBody should include regex routes.BecomeLeadController.getAcceptResponsibilities().url
                  htmlBody should include regex routes.FinancialDashboardController.getFinancialDashboard.url
                  htmlBody should include regex routes.RemoveYourselfBusinessEntityController.getRemoveYourselfBusinessEntity.url
                  verifyUndertakingBalance(doc)
                }
              )
            }

          }
        }

        "display the account home page with warning when undertaking is auto suspended" when {
          "admin page loads home - 'undertakingStatus == active'" in {
            val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(
                undertaking.copy(undertakingStatus = Some(UndertakingStatus.active)).some.toFuture
              )
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
              mockTimeProviderToday(fixedDate)
              mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                undertakingSubsidies.toFuture
              )
              mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
              mockTimeProviderToday(fixedDate)
              mockGetOrCreate(eori1)(Right(nilJourneyCreate))
            }

            val result = performAction()

            val doc = Jsoup.parse(contentAsString(result))

            verifyGenericHomepageContentForLead(doc)
            verifyPreDeadlineContentForLead(doc)
          }
          "admin page loads home - 'undertakingStatus == suspendedAutomated'" in {
            val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(
                undertaking.copy(undertakingStatus = Some(UndertakingStatus.suspendedAutomated)).some.toFuture
              )
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
              mockTimeProviderToday(fixedDate)
              mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                undertakingSubsidies.toFuture
              )
              mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
              mockTimeProviderToday(fixedDate)
              mockGetOrCreate(eori1)(Right(nilJourneyCreate))
            }

            val result = performAction()

            val doc = Jsoup.parse(contentAsString(result))

            verifyGenericHomepageContentForLead(doc)
            verifyAutoSuspendContentForLead(doc)
          }
          "member page loads home" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification(eori4)
              mockRetrieveUndertaking(eori4)(
                undertaking1.copy(undertakingStatus = Some(UndertakingStatus.suspendedAutomated)).some.toFuture
              )
              mockGetOrCreate[EligibilityJourney](eori4)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori4)(Right(UndertakingJourney()))
              mockTimeProviderToday(fixedDate)
              mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                undertakingSubsidies.toFuture
              )
              mockGetUndertakingBalance(eori4)(Future.successful(Some(undertakingBalance)))
              mockTimeProviderToday(fixedDate)
            }

            val result = performAction()

            val doc = Jsoup.parse(contentAsString(result))

            doc.title() shouldBe "Your undertaking - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
            doc
              .getElementById("warning-text")
              .text shouldBe "! Warning Your undertaking's deadline to submit a report passed on 18 April 2021."

          }
        }

        "display the account home page with message about scp08 issues" when {
          "admin page loads home" in {
            val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockRetrieveUndertaking(eori1)(
                undertaking.some.toFuture
              )
              mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
              mockTimeProviderToday(fixedDate)
              mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                undertakingSubsidies.toFuture
              )
              mockGetUndertakingBalance(eori1)(Future.successful(None))
              mockTimeProviderToday(fixedDate)
              mockGetOrCreate(eori1)(Right(nilJourneyCreate))
            }

            val result = performAction()

            val doc = Jsoup.parse(contentAsString(result))

            verifyScp08IssuesMessage(doc)
          }
          "member page loads home" in {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification(eori4)
              mockRetrieveUndertaking(eori4)(
                undertaking1.some.toFuture
              )
              mockGetOrCreate[EligibilityJourney](eori4)(Right(eligibilityJourneyComplete))
              mockGetOrCreate[UndertakingJourney](eori4)(Right(UndertakingJourney()))
              mockTimeProviderToday(fixedDate)
              mockRetrieveSubsidiesForDateRange(undertakingRef, fixedDate.toSearchRange)(
                undertakingSubsidies.toFuture
              )
              mockGetUndertakingBalance(eori4)(Future.successful(None))
              mockTimeProviderToday(fixedDate)
            }

            val result = performAction()

            val doc = Jsoup.parse(contentAsString(result))

            verifyScp08IssuesMessage(doc)
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
              checkIsRedirect(performAction(), routes.EligibilityFirstEmptyPageController.firstEmptyPage.url)
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
              checkIsRedirect(performAction(), routes.EligibilityFirstEmptyPageController.firstEmptyPage)
            }

            "eligibility Journey  is complete and undertaking Journey is not complete" in {
              inSequence {
                mockAuthWithEnrolmentAndNoEmailVerification()
                mockRetrieveUndertaking(eori1)(None.toFuture)
                mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
                mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
              }
              checkIsRedirect(performAction(), routes.UndertakingController.firstEmptyPage)
            }

            "eligibility Journey  and undertaking Journey are  complete" in {
              inSequence {
                mockAuthWithEnrolmentAndNoEmailVerification()
                mockRetrieveUndertaking(eori1)(None.toFuture)
                mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
                mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete1))
              }
              checkIsRedirect(performAction(), routes.AddBusinessEntityController.getAddBusinessEntity())
            }
          }

        }

      }
    }
  }

  private def verifyScp08IssuesMessage(doc: Document) = {
    doc
      .getElementById("scp08Issues-text")
      .text shouldBe "Your undertaking balance may show a temporary difference to your own records, which will be amended here within 24 hours."
  }

  private def verifyUndertakingBalance(doc: Document): Unit = {
    doc.getElementById("undertaking-balance-section-heading").text shouldBe "Undertaking balance"
    doc
      .getElementById("undertaking-balance-section-content")
      .text shouldBe "Your undertaking currently has a remaining balance of €123.45, from your sector allowance of €12.34."
  }

  private def verifyUndertakingBalanceAgriOther(doc: Document): Unit = {
    doc.getElementById("undertaking-balance-section-heading").text shouldBe "Undertaking balance"
    doc
      .getElementById("undertaking-balance-section-content-agri-other")
      .text shouldBe "Your balance may be incorrect. To work out your balance, review changes to your allowance."
  }

  private def verifyAutoSuspendContentForLead(doc: Document) = {
    doc
      .getElementById("warning-text")
      .text shouldBe "! Warning Your deadline to submit a report passed on 18 April 2021."
    doc
      .getElementById("lead-account-homepage-p1")
      .text shouldBe "This date was 90 days after you either:"
    doc
      .getElementById("lead-account-homepage-p2")
      .text shouldBe "You must now either:"
    doc
      .getElementById("lead-account-homepage-h2")
      .text shouldBe "What you need to report now"
    doc
      .getElementById("lead-account-homepage-h2-p1")
      .text shouldBe "You must use this service to report all non-customs subsidy payments you have received to continue claiming de minimis state aid."
    doc
      .getElementById("lead-account-homepage-h2-p2")
      .text shouldBe "These payments can include grants and loans, provided as de minimis state aid from government and public authorities. You must report these at least once every 90 days, even if you have not received any payments."
    doc
      .getElementById("lead-account-homepage-h2-p3")
      .text shouldBe "If you have received none, you must submit a report that you have received no payments for that period."
  }

  private def verifyPreDeadlineContentForLead(doc: Document) = {
    doc
      .getElementById("warning-text") shouldBe null
    doc
      .getElementById("lead-account-homepage-p1")
      .text shouldBe "This date is 90 days after you either:"
    doc
      .getElementById("lead-account-homepage-p2")
      .text shouldBe "You must either:"
    doc
      .getElementById("lead-account-homepage-h2")
      .text shouldBe "What you need to report, and when"
    doc
      .getElementById("lead-account-homepage-h2-p1")
      .text shouldBe "You must use this service to report all non-customs subsidy payments you have received."
    doc
      .getElementById("lead-account-homepage-h2-p2")
      .text shouldBe "You must submit a report at least once every 90 days, even if you have not received any payments."
    doc
      .getElementById("lead-account-homepage-h2-p3")
      .text shouldBe "If you do not submit a report, your undertaking may no longer be able to claim Customs Duty waivers."
  }
}
