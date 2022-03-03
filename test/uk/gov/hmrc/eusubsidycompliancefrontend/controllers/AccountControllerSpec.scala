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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, EscService, RetrieveEmailService, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{undertaking, _}

import java.time.LocalDate
import scala.concurrent.Future

class AccountControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour {

  val mockEscService           = mock[EscService]
  val mockRetrieveEmailService = mock[RetrieveEmailService]
  val mockTimeProvider         = mock[TimeProvider]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[RetrieveEmailService].toInstance(mockRetrieveEmailService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  val controller = instanceOf[AccountController]

  def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockRetrieveEmail(eori: EORI)(result: Either[Error, Option[EmailAddress]]) =
    (mockRetrieveEmailService
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning {
        result
          .fold(
            e =>
              Future.failed(
                e.value
                  .fold(s => new Exception(s), identity)
              ),
            Future.successful
          )
      }

  private def mockTimeProviderToday(today: LocalDate) =
    (mockTimeProvider.today _).expects().returning(today)

  "AccountController" when {

    "handling request to get Account page" must {

      def performAction() = controller.getAccountPage(FakeRequest())

      behave like authBehaviour(() => performAction())

      "display the page" when {

        def test(undertaking: Undertaking) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Right(undertaking))
            mockTimeProviderToday(fixedDate)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title", undertaking.name),
            { doc =>
              val htmlBody = doc.select(".govuk-grid-column-one-third").html()

              htmlBody should include regex messageFromMessageKey(
                "lead-account-homepage.cards.card1.link1",
                routes.SubsidyController.getReportPayment().url
              )

              htmlBody should include regex messageFromMessageKey(
                "lead-account-homepage.cards.card2.link1",
                routes.UndertakingController.getAmendUndertakingDetails().url
              )

              htmlBody should include regex messageFromMessageKey(
                "lead-account-homepage.cards.card2.link2",
                routes.FinancialDashboardController.getFinancialDashboard().url
              )

              if (undertaking.undertakingBusinessEntity.length > 1)
                htmlBody should include regex messageFromMessageKey(
                  "lead-account-homepage.cards.card3.link1View",
                  routes.BusinessEntityController.getAddBusinessEntity().url
                )
              else
                htmlBody should include regex messageFromMessageKey(
                  "lead-account-homepage.cards.card3.link1Add",
                  routes.BusinessEntityController.getAddBusinessEntity().url
                )

              val isNonLeadEORIPresent = !undertaking.undertakingBusinessEntity.forall(_.leadEORI)

              if (isNonLeadEORIPresent)
                htmlBody should include regex messageFromMessageKey(
                  "lead-account-homepage.cards.card3.link2",
                  routes.SelectNewLeadController.getSelectNewLead().url
                )
              else
                htmlBody should include regex messageFromMessageKey(
                  "lead-account-homepage.cards.card3.link2",
                  routes.NoBusinessPresentController.getNoBusinessPresent().url
                )

            }
          )
        }

        def testTimeToReport(
          undertaking: Undertaking,
          currentDate: LocalDate,
          isTimeToReport: Boolean,
          dueDate: String,
          isOverdue: Boolean
        ) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Right(undertaking))
            mockTimeProviderToday(currentDate)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("lead-account-homepage.title", undertaking.name),
            doc =>
              if (isTimeToReport) {
                val htmlBody = doc.select(".govuk-inset-text").text
                htmlBody should include regex
                  messageFromMessageKey("lead-account-homepage.inset", dueDate)
              } else if (isOverdue) {
                val htmlBody = doc.select(".govuk-inset-text").text
                htmlBody should include regex
                  messageFromMessageKey("lead-account-homepage-overdue.inset", dueDate)
              }
          )
        }

        "there is a view link on the page and undertaking has lead only business entity" in {
          test(undertaking)
        }

        "there is a add link on the page" in {
          test(undertaking.copy(undertakingBusinessEntity = List(businessEntity1)))
        }

        "The undertaking  any non-Lead  business entities " in {
          test(undertaking1)
        }

        "today's date falls between the 76th and the 90th day from the last day of subsidy report " in {
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = LocalDate.of(2021, 12, 1).some),
            currentDate = LocalDate.of(2022, 2, 16),
            true,
            "1 March 2022",
            false
          )
        }
        "today's date is exactly 76 days from the last day of subsidy report " in {
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = LocalDate.of(2021, 12, 1).some),
            currentDate = LocalDate.of(2022, 2, 15),
            true,
            "1 March 2022",
            false
          )
        }

        "today's over 90 days from the last day of subsidy report " in {
          val lastUpdatedDate = LocalDate.of(2021, 12, 1)
          testTimeToReport(
            undertaking.copy(lastSubsidyUsageUpdt = lastUpdatedDate.some),
            currentDate = lastUpdatedDate.plusDays(91),
            false,
            "1 March 2022",
            true
          )
        }

      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the email" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching eligibility journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing eligibility journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(None))
            mockPut[EligibilityJourney](EligibilityJourney(), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in retrieving  undertaking  journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing  undertaking  journey data" in {

          val undertakingData = UndertakingJourney.fromUndertakingOpt(None)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(None))
            mockPut[UndertakingJourney](undertakingData, eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching Business entity  journey data" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing Business entity  journey data" in {
          val businessEntityData = BusinessEntityJourney.fromUndertakingOpt(None)

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(None))
            mockPut[BusinessEntityJourney](businessEntityData, eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing Undertaking data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Left(Error(exception)))

          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "redirect to next page" when {

        "No email is retrieved from cds api" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveEmail(eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UpdateEmailAddressController.updateEmailAddress())
        }

        "email is retrieved from cds api" when {

          "there is no existing retrieve undertaking" when {

            "eligibility Journey  is not complete and undertaking Journey is blank" in {
              inSequence {
                mockAuthWithNecessaryEnrolment()
                mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
                mockRetreiveUndertaking(eori1)(Future.successful(None))
                mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
                mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
                mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
              }
              checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage())
            }

            "eligibility Journey  is complete and undertaking Journey is not complete" in {
              inSequence {
                mockAuthWithNecessaryEnrolment()
                mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
                mockRetreiveUndertaking(eori1)(Future.successful(None))
                mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
                mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
                mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
              }
              checkIsRedirect(performAction(), routes.UndertakingController.firstEmptyPage())
            }

            "eligibility Journey  and undertaking Journey are  complete" in {
              inSequence {
                mockAuthWithNecessaryEnrolment()
                mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
                mockRetreiveUndertaking(eori1)(Future.successful(None))
                mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
                mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete1.some))
                mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
              }
              checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity())
            }
          }
        }

      }

    }

    "handling request to get existing undertaking page" must {

      def performAction() = controller.getExistingUndertaking()(FakeRequest())

      behave like authBehaviour(() => performAction())

      "throw a technical error page" when {

        "retrieved undertaking has lead EORI missing" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(Some(undertaking2)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" in {
        inSequence {
          mockAuthWithBEEnrolment()
          mockRetreiveUndertaking(eori4)(Future.successful(Some(undertaking1)))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("existingUndertaking.title"),
          { doc =>
            doc.select("h2").get(0).text shouldBe undertaking1.name
            val htmlBody = doc.select(".govuk-list").html()
            htmlBody should include regex messageFromMessageKey(
              "existingUndertaking.link1",
              routes.AccountController.getAccountPage().url
            )

            htmlBody should include regex messageFromMessageKey(
              "existingUndertaking.link2",
              routes.BusinessEntityController.getRemoveYourselfBusinessEntity().url
            )

          }
        )

      }

      "redirect to next page" when {

        def redirectTest(eori: EORI, undertakingOpt: Option[Undertaking], nextCall: String) = {

          inSequence {
            mockAuthWithEnrolment(eori)
            mockRetreiveUndertaking(eori)(Future.successful(undertakingOpt))
          }
          checkIsRedirect(performAction(), nextCall)

        }

        "user with non-lead EORI logged in and has no previous undertaking" in {
          redirectTest(eori4, None, routes.AccountController.getAccountPage().url)
        }

        "user with lead EORI is logged in and has no previous undertaking" in {
          redirectTest(eori1, None, routes.AccountController.getAccountPage().url)
        }

        "user with lead EORI is logged in and has  previous undertaking" in {
          redirectTest(eori1, undertaking1.some, routes.AccountController.getAccountPage().url)
        }

      }

    }

  }

}
