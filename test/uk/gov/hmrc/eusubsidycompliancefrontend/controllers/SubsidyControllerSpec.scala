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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyControllerSpec.RemoveSubsidyRow
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.{IncorrectFormat, Required}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.{ClaimAmountFormProvider, ClaimEoriFormProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyRemoved
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyRef
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class SubsidyControllerSpec
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
    bind[EmailVerificationService].toInstance(authSupport.mockEmailVerificationService),
    bind[Store].toInstance(journeyStoreSupport.mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  private val controller = instanceOf[SubsidyController]
  private val exception = new Exception("oh no!")
  private val currentDate = LocalDate.of(2022, 10, 9)

  private val dateRange = (LocalDate.of(2020, 4, 6), LocalDate.of(2022, 10, 9))

  "SubsidyControllerSpec" when {

    "handling request to get reported payments page" must {

      def performAction =
        controller.getReportedPayments(FakeRequest(GET, routes.SubsidyController.getReportedPayments.url))

      "throw technical error" when {
        "call to get undertaking from EIS fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(Future.failed(new RuntimeException("Oh no!")))
          }

          assertThrows[Exception](await(performAction))
        }

        "call to get subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {

        val previousUrl = routes.AccountController.getAccountPage.url

        def test(nonHMRCSubsidyUsage: List[NonHmrcSubsidy]) = {
          val subsidies = undertakingSubsidies.copy(nonHMRCSubsidyUsage = nonHMRCSubsidyUsage)
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(subsidies.toFuture)
          }
        }

        "user has not reported any payments" in {
          test(List.empty)
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("reportedPayments.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              doc.select("#subsidy-list").size() shouldBe 0
            }
          )
        }

        "user has reported at least one payment" in {
          test(nonHmrcSubsidyList.map(_.copy(subsidyUsageTransactionId = SubsidyRef("Z12345").some)))
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("reportedPayments.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              val subsidyList = doc.select("#subsidy-list")

              subsidyList.select("thead > tr > th:nth-child(1)").text() shouldBe "Date"
              subsidyList.select("thead > tr > th:nth-child(2)").text() shouldBe "Amount"
              subsidyList.select("thead > tr > th:nth-child(3)").text() shouldBe "EORI number"
              subsidyList.select("thead > tr > th:nth-child(4)").text() shouldBe "Public authority"
              subsidyList.select("thead > tr > th:nth-child(5)").text() shouldBe "Your reference"

              subsidyList.select("tbody > tr > td:nth-child(1)").text() shouldBe "1 Jan 2022"
              subsidyList.select("tbody > tr > td:nth-child(2)").text() shouldBe "€543.21"
              subsidyList.select("tbody > tr > td:nth-child(3)").text() shouldBe "GB123456789012"
              subsidyList.select("tbody > tr > td:nth-child(4)").text() shouldBe "Local Authority"
              subsidyList.select("tbody > tr > td:nth-child(5)").text() shouldBe "ABC123"
              subsidyList.select("tbody > tr > td:nth-child(6)").text() shouldBe "Remove payment, dated 1 Jan 2022"

              subsidyList.select("tbody > tr > td:nth-child(6) > a").attr("href") shouldBe routes.SubsidyController
                .getRemoveSubsidyClaim("Z12345")
                .url
            }
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }

    }

    "handling request to get claim date page" must {

      def performAction = controller.getClaimDate(FakeRequest(GET, routes.SubsidyController.getClaimDate.url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get session fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))

        }

      }

      "display the page" when {

        "a valid request is made" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeProviderToday(fixedDate)
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-date.title"),
            { doc =>
              doc.select("#claim-date > div:nth-child(1) > div > label").text() shouldBe "Day"
              doc.select("#claim-date > div:nth-child(2) > div > label").text() shouldBe "Month"
              doc.select("#claim-date > div:nth-child(3) > div > label").text() shouldBe "Year"
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postClaimDate.url
            }
          )
        }

      }

      "redirect " when {

        "user is not an undertaking lead to account home page" in {
          testLeadOnlyRedirect(() => performAction)
        }

      }
    }

    "handling request to post claim date" must {

      def performAction(data: (String, String)*) = controller.postClaimDate(
        FakeRequest(POST, routes.SubsidyController.postClaimDate.url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {
        "entered date is valid" in {
          val updatedDate = DateFormValues("1", "2", LocalDate.now().getYear.toString)
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(
              Right(SubsidyJourney(claimDate = ClaimDateFormPage(updatedDate.some)))
            )
          }
          checkIsRedirect(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year),
            routes.SubsidyController.getClaimAmount.url
          )
        }
      }

      "return to claim date page" when {
        "entered date is not valid" in {
          val updatedDate = DateFormValues("20", "20", LocalDate.now().getYear.toString)
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockTimeProviderToday(fixedDate)
          }
          status(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year)
          ) shouldBe BAD_REQUEST
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling request to get claim amount" must {

      def performAction = controller
        .getClaimAmount(FakeRequest(GET, routes.SubsidyController.getClaimAmount.url))

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to get subsidy journey fails " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }
      "redirect" when {
        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          redirectLocation(performAction) shouldBe Some(routes.SubsidyController.getClaimDate.url)
        }
      }

      "display the page" when {

        val claimAmountEurosId = "claim-amount-eur"
        val claimAmountPoundsId = "claim-amount-gbp"

        def test(subsidyJourney: SubsidyJourney, elementId: String): Unit = {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))

          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-amount.title"),
            { doc =>
              val input = doc.getElementById(elementId).attributes().get("value")
              input shouldBe subsidyJourney.claimAmount.value.map(_.amount).getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimAmount.url

            }
          )
        }

        "user hasn't already answered the question - EUR input field should be empty" in {
          test(
            SubsidyJourney(
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
            ),
            claimAmountEurosId
          )
        }

        "user hasn't already answered the question - GBP input field should be empty" in {
          test(
            SubsidyJourney(
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
            ),
            claimAmountPoundsId
          )
        }

        "user has already entered a EUR amount" in {
          test(
            SubsidyJourney(
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
              claimAmount = ClaimAmountFormPage(value = claimAmountEuros.some)
            ),
            claimAmountEurosId
          )
        }

        "user has already entered a GBP amount" in {
          test(
            SubsidyJourney(
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
              claimAmount = ClaimAmountFormPage(value = claimAmountPounds.some)
            ),
            claimAmountPoundsId
          )
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }

    }

    "handling request to post claim amount" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimAmount(
          FakeRequest(POST, routes.SubsidyController.getClaimAmount.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to get subsidy journey fails " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back empty " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back with No claim date " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the subsidy journey fails" in {

          val subsidyJourney = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
          )

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](
            await(
              performAction(
                ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "123.45",
                ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName
              )
            )
          )
        }
      }

      "display form error" when {

        def testFormValidation(data: (String, String)*)(errorMessageKey: String): Unit = {

          val subsidyJourneyOpt = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          ).some
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
          }

          val titleMessage = messageFromMessageKey("add-claim-amount.title")
          val errorMessage = messageFromMessageKey(errorMessageKey)

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            titleMessage,
            errorMessage
          )
        }

        def testConvertedAmountValidation(data: (String, String)*)(errorMessageKey: String): Unit = {

          val subsidyJourneyOpt = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
          ).some
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
          }

          val titleMessage = messageFromMessageKey("add-claim-amount.title")
          val errorMessage = messageFromMessageKey(errorMessageKey)

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            titleMessage,
            errorMessage
          )
        }

        "no currency code has been selected" in {
          testFormValidation()("add-claim-amount.currency-code.error.required")
        }

        "nothing is entered" in {
          CurrencyCode.values.foreach { c =>
            testFormValidation(
              ClaimAmountFormProvider.Fields.CurrencyCode -> c.entryName
            )(s"add-claim-amount.claim-amount-${c.entryName.toLowerCase}.$Required")
          }
        }

        "claim amount entered in wrong format" in {
          CurrencyCode.values.foreach { c =>
            testFormValidation(
              ClaimAmountFormProvider.Fields.CurrencyCode -> c.entryName,
              ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "123.4"
            )(s"add-claim-amount.claim-amount-${c.entryName.toLowerCase}.$IncorrectFormat")
          }
        }

        "claim amount entered in GBP is too big" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "£123456789012.12"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.$TooBig")
        }

        "claim amount entered in EUR is too big" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "€123456789012.12"
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.$TooBig")
        }

        "claim amount entered in GBP is too small" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "0.00"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.$TooSmall")
        }

        "claim amount entered in EUR is too small" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "0.00"
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.$TooSmall")
        }

        "claim amount claims to be euros, but submits value with pound sign prefix" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "£99.99"
          )(s"add-claim-amount.claim-amount-eur.error.incorrect-format")
        }

        "claim amount claims to be gbp, but submits value with eur sign prefix" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "€99.99"
          )(s"add-claim-amount.claim-amount-gbp.error.incorrect-format")
        }

        "claim amount entered in GBP exceeds maximum allowed value in EUR when converted" in {
          testConvertedAmountValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "£999999.99"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.$TooBig")
        }

      }

      "redirect to the add claim eori page if an EUR amount is entered" in {
        val subsidyJourney = SubsidyJourney(
          claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
        )

        val claimAmount = "100.00"

        val subsidyJourneyWithClaimAmount = subsidyJourney.copy(
          claimAmount = ClaimAmountFormPage(ClaimAmount(EUR, claimAmount).some)
        )

        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(subsidyJourneyWithClaimAmount))
        }

        checkIsRedirect(
          performAction(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> claimAmount
          ),
          routes.SubsidyController.getAddClaimEori.url
        )
      }

      "redirect to the confirm converted amount page if a GBP amount is entered" in {
        val subsidyJourney = SubsidyJourney(
          claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
        )

        val claimAmount = "100.00"

        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
          journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(subsidyJourney))
        }

        checkIsRedirect(
          performAction(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> claimAmount
          ),
          routes.SubsidyController.getConfirmClaimAmount.url
        )

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get claim amount currency conversion page" when {

      def performAction() = controller.getConfirmClaimAmount(
        FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount.url)
      )

      "throw a technical error" when {

        "the call to fetch the subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(
              Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some)
            )
            mockRetrieveExchangeRate(claimDate)(Future.failed(exception))
          }

          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "a successful request is made" in {

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(
              Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some)
            )
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
          }

          checkPageIsDisplayed(
            result = performAction(),
            expectedTitle = messageFromMessageKey("confirm-converted-amount.title")
          )

        }

      }

      "redirect" when {

        "user is not an undertaking lead, to the account home page" in {
          testLeadOnlyRedirect(() => performAction())
        }

      }

    }

    "handling request to post claim amount currency confirmation" must {

      def performAction() = controller.postConfirmClaimAmount(
        FakeRequest(POST, routes.SubsidyController.postConfirmClaimAmount.url)
      )

      "throw technical error" when {

        "the call to fetch the subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(
              Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some)
            )
            mockRetrieveExchangeRate(claimDate)(Future.failed(exception))
          }

          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect to next page" when {

        "the user submits the form to accept the exchange rate" in {
          val initialJourney = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some),
            claimAmount = ClaimAmountFormPage(claimAmountPounds.some)
          )

          val updatedJourney = initialJourney.copy(
            convertedClaimAmountConfirmation = ConvertedClaimAmountConfirmationPage(ClaimAmount(EUR, "138.55").some)
          )

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(initialJourney.some))
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
            journeyStoreSupport.mockPut[SubsidyJourney](updatedJourney, eori1)(Right(updatedJourney))
          }

          val result = performAction()
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)
        }

      }

    }

    "handling request to get Add Claim Eori" must {

      def performAction = controller
        .getAddClaimEori(FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "the call to get subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-eori.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.addClaimEori.value match {
                case Some(OptionalClaimEori(input, eori, _)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe eori.map(_.drop(2)).getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimEori.url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(None)))
        }

        "the user has already answered the question" in {
          List(
            subsidyJourney,
            subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(OptionalClaimEori("false", None).some))
          )
            .foreach { subsidyJourney =>
              withClue(s" for each subsidy journey $subsidyJourney") {
                test(subsidyJourney)
              }
            }
        }

      }

      "redirect" when {
        "user is not an undertaking lead, to the account home page" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }
    }

    "handling the request to post add claim eori" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimEori(
          FakeRequest(POST, routes.SubsidyController.getAddClaimEori.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("should-claim-eori" -> "false")))
        }
      }

      "show form error" when {

        def testFormError(
          inputAnswer: Option[List[(String, String)]],
          errorMessageKey: String
        ): Unit = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkFormErrorIsDisplayed(
            performAction(answers: _*),
            messageFromMessageKey("add-claim-eori.title"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "should-claim-eori.error.required")
        }

        "yes is selected but no eori is entered" in {
          testFormError(Some(List("should-claim-eori" -> "true")), s"claim-eori.$Required")

        }

        "yes is selected but eori entered is invalid" in {
          testFormError(
            Some(List("should-claim-eori" -> "true", "claim-eori" -> "GB1234567890")),
            s"claim-eori.$IncorrectFormat"
          )

        }

        "yes is selected but eori entered is part of another undertaking" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockRetrieveUndertaking(eori3)(undertaking.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction("should-claim-eori" -> "true", "claim-eori" -> eori3),
            messageFromMessageKey("add-claim-eori.title"),
            messageFromMessageKey("claim-eori." + ClaimEoriFormProvider.Errors.InAnotherUndertaking)
          )
        }

      }

      "redirect to next page" when {

        val journey = subsidyJourney.copy(
          traderRef = TraderRefFormPage()
        )

        def update(subsidyJourney: SubsidyJourney, formValues: Option[OptionalClaimEori]) =
          subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(formValues))

        def testRedirect(optionalEORI: OptionalClaimEori, inputAnswer: List[(String, String)]): Unit = {
          val updatedSubsidyJourney = update(journey, optionalEORI.some)

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(journey.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(updatedSubsidyJourney))
          }
          checkIsRedirect(performAction(inputAnswer: _*), routes.SubsidyController.getAddClaimPublicAuthority.url)
        }

        "user selected yes and entered a valid eori part of the existing undertaking" in {
          testRedirect(
            OptionalClaimEori("true", "123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "123456789013")
          )
        }

        "user selected yes and entered a valid eori with GB prefix part of the existing undertaking" in {
          testRedirect(
            OptionalClaimEori("true", "GB123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "GB123456789013")
          )
        }

        "user selected yes and entered a valid EORI that is not part of the existing or any other undertaking" in {
          val optionalEORI = OptionalClaimEori("true", eori3.some)

          val updatedSubsidyJourney = update(journey, optionalEORI.copy(addToUndertaking = true).some)

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(journey.some))
            mockRetrieveUndertaking(eori3)(Option.empty.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(updatedSubsidyJourney))
          }

          checkIsRedirect(
            performAction("should-claim-eori" -> optionalEORI.setValue, "claim-eori" -> eori3),
            routes.SubsidyController.getAddClaimBusiness.url
          )
        }

        "user selected no " in {
          testRedirect(OptionalClaimEori("false", None), List("should-claim-eori" -> "false"))
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get add claim public authority" must {

      def performAction = controller.getAddClaimPublicAuthority(
        FakeRequest(
          GET,
          routes.SubsidyController.getAddClaimPublicAuthority.url
        )
      )

      "display the page" when {
        "a valid request is made" in {
          val incompleteJourney = subsidyJourney.copy(traderRef = TraderRefFormPage())

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Right(incompleteJourney))
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-public-authority.title"),
            { doc =>
              doc.select("#claim-public-authority-hint").text() shouldBe "For example, Invest NI, NI Direct"
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimPublicAuthority.url
            }
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }
    }

    "handling request to post add claim public authority" must {

      def performAction(data: (String, String)*) = controller.postAddClaimPublicAuthority(
        FakeRequest(POST, routes.SubsidyController.postAddClaimPublicAuthority.url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        val journey = subsidyJourney.copy(traderRef = TraderRefFormPage())

        "valid input" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(journey.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(
              Right(journey.copy(publicAuthority = PublicAuthorityFormPage(Some("My Authority"))))
            )
          }
          checkIsRedirect(
            performAction("claim-public-authority" -> "My Authority"),
            routes.SubsidyController.getAddClaimReference.url
          )
        }
      }

      "show form error" when {

        def displayError(data: (String, String)*)(messageKey: String): Unit = {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("add-claim-public-authority.title"),
            messageFromMessageKey(messageKey)
          )
        }

        "nothing is entered" in {
          displayError("claim-public-authority" -> "")("error.claim-public-authority.required")

        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling request to get Add Claim Reference" must {
      def performAction = controller
        .getAddClaimReference(FakeRequest(GET, routes.SubsidyController.getAddClaimReference.url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "the call to get subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-trader-reference.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.traderRef.value match {
                case Some(OptionalTraderRef(input, traderRef)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe traderRef.getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimReference.url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(
            subsidyJourney.copy(
              traderRef = TraderRefFormPage(),
              cya = CyaFormPage()
            )
          )
        }

        "the user has already answered the question" in {
          List(
            subsidyJourney,
            subsidyJourney.copy(traderRef = TraderRefFormPage(OptionalTraderRef("false", None).some))
          )
            .foreach { subsidyJourney =>
              withClue(s" for each subsidy journey $subsidyJourney") {
                test(subsidyJourney)
              }

            }

        }

      }

      "redirect " when {

        "user is not an undertaking lead, to the account home page" in {
          testLeadOnlyRedirect(() => performAction)
        }

      }

    }

    "handling request to post Add Claim Reference" must {
      def performAction(data: (String, String)*) = controller
        .postAddClaimReference(
          FakeRequest(POST, routes.SubsidyController.getAddClaimReference.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "show form error" when {

        def testFormError(inputAnswer: Option[List[(String, String)]], errorMessageKey: String): Unit = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkFormErrorIsDisplayed(
            performAction(answers: _*),
            messageFromMessageKey("add-claim-trader-reference.title"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "should-store-trader-ref.error.required")
        }

        "yes is selected but no trader reference is added" in {
          testFormError(Some(List("should-store-trader-ref" -> "true")), "claim-trader-ref.error.required")
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get remove subsidy claim" must {

      val transactionId = "TID1234"

      def performAction(transactionId: String) = controller
        .getRemoveSubsidyClaim(transactionId)(
          FakeRequest(GET, routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
        )

      "throw technical error" when {

        "call to fetch undertaking passes but come back with empty reference" in {

          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy failed" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy comes back with nonHMRC subsidy usage list with missing transactionId" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies.toFuture)
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }
      }

      "display the page" in {

        def expectedRows(nonHmrcSubsidy: NonHmrcSubsidy) = List(
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.claimDate.key"),
            nonHmrcSubsidy.allocationDate.toDisplayFormat
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.amount.key"),
            nonHmrcSubsidy.nonHMRCSubsidyAmtEUR.toEuros
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.claim.key"),
            nonHmrcSubsidy.businessEntityIdentifier.getOrElse("")
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.authority.key"),
            nonHmrcSubsidy.publicAuthority.getOrElse("")
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.traderRef.key"),
            nonHmrcSubsidy.traderReference.getOrElse("")
          )
        )

        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(currentDate)
          mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
        }

        checkPageIsDisplayed(
          performAction(transactionId),
          messageFromMessageKey("subsidy.remove.title"),
          { doc =>
            val rows =
              doc.select(".govuk-summary-list__row").asScala.toList.map { element =>
                val question = element.select(".govuk-summary-list__key").text()
                val answer = element.select(".govuk-summary-list__value").text()
                RemoveSubsidyRow(question, answer)
              }
            rows shouldBe expectedRows(nonHmrcSubsidyList1.head)
            val button = doc.select("form")
            button.attr("action") shouldBe routes.SubsidyController.postRemoveSubsidyClaim(transactionId).url

          }
        )
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction(""))
        }
      }

    }

    "handling post remove subsidy claim" must {

      def performAction(data: (String, String)*)(transactionId: String) = controller
        .postRemoveSubsidyClaim(transactionId)(
          FakeRequest(POST, routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        "call to retrieve subsidy fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to remove subsidy fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

      }

      "display page error" when {

        "nothing is submitted" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()("TID1234"),
            messageFromMessageKey("subsidy.remove.title"),
            messageFromMessageKey("subsidy.remove.error.required")
          )
        }
      }

      "display reported payments page with successful removal banner" when {

        "If user select yes" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Right(undertakingRef))
            mockSendAuditEvent[NonCustomsSubsidyRemoved](AuditEvent.NonCustomsSubsidyRemoved("1123", undertakingRef))
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
          }

          val result = performAction("removeSubsidyClaim" -> "true")("TID1234")

          status(result) shouldBe OK
          contentAsString(result) should include(messages("reportedPayments.removed"))
        }

      }

      "redirect to next page" when {

        "if user selects no" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeSubsidyClaim" -> "false")("TID1234"),
            routes.SubsidyController.getReportedPayments.url
          )

        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(""))
        }
      }
    }

    "handling get of check your answers" must {

      def performAction() = controller.getCheckAnswers(
        FakeRequest(GET, routes.SubsidyController.getCheckAnswers.url)
      )

      "throw technical error" when {

        def testErrorFor(s: SubsidyJourney) = {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(s.some))
          }

          assertThrows[Exception](await(performAction()))
        }

        "EORI is not present on subsidy journey" in {
          testErrorFor(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage()))
        }

        "claim amount is not present on subsidy journey" in {
          testErrorFor(subsidyJourney.copy(claimAmount = ClaimAmountFormPage()))
        }

        "claim date is not present on subsidy journey" in {
          testErrorFor(subsidyJourney.copy(claimDate = ClaimDateFormPage()))
        }

        "public authority is not present on subsidy journey" in {
          testErrorFor(subsidyJourney.copy(publicAuthority = PublicAuthorityFormPage()))
        }

        "trader ref is not present on subsidy journey" in {
          testErrorFor(subsidyJourney.copy(traderRef = TraderRefFormPage()))
        }

      }

      "redirect to account home if no subsidy journey data found" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(Option.empty))
        }

        val result = performAction()

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
      }

      "display the page" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
        }

        status(performAction()) shouldBe OK
      }

    }

    "handling post to check your answers" must {

      def performAction(data: (String, String)*) = controller
        .postCheckAnswers(
          FakeRequest(POST, routes.SubsidyController.getCheckAnswers.url)
            .withFormUrlEncodedBody(data: _*)
        )

      val updatedJourney = subsidyJourney.copy(cya = CyaFormPage(value = true.some))

      "throw technical error" when {

        "call to update subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to create subsidy fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(updatedJourney))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to delete subsidy journey fails" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(updatedJourney))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Right(undertakingRef))
            journeyStoreSupport.mockDelete[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }
      }

      "redirect to next page" when {

        "cya page is reached via add journey" in {

          val updatedSJ = subsidyJourney.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(updatedSJ))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
            journeyStoreSupport.mockDelete[SubsidyJourney](eori1)(Right(SubsidyJourney()))
            mockSendAuditEvent[AuditEvent.NonCustomsSubsidyAdded](
              AuditEvent.NonCustomsSubsidyAdded(
                ggDetails = "1123",
                leadEori = eori1,
                undertakingRef = undertakingRef,
                subsidyJourney = updatedSJ,
                currentDate
              )
            )
          }

          checkIsRedirect(
            performAction("cya" -> "true"),
            routes.SubsidyController.getClaimConfirmationPage.url
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling get of claim confirmation" must {
      def performAction() = controller.getClaimConfirmationPage(
        FakeRequest(GET, routes.SubsidyController.getClaimConfirmationPage.url)
      )

      "display the page" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockTimeProviderToday(fixedDate)
        }

        val result = performAction()

        status(result) shouldBe OK

        val expectedDeadlineDate = "20 April 2021"
        contentAsString(result) should include(expectedDeadlineDate)
      }

    }

    "handling get of add claim business page" must {
      def performAction() = controller.getAddClaimBusiness(
        FakeRequest(GET, routes.SubsidyController.getClaimConfirmationPage.url)
      )

      "display the page" in {
        inSequence {
          authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          journeyStoreSupport.mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
        }

        val result = performAction()

        status(result) shouldBe OK

        contentAsString(result) should include("Do you want to add this business to your undertaking?")
      }

    }

    "handling post to add claim business page" must {
      def performAction(form: (String, String)*) = controller.postAddClaimBusiness(
        FakeRequest(POST, routes.SubsidyController.postAddClaimBusiness.url)
          .withFormUrlEncodedBody(form: _*)
      )

      "redirect to next page" when {

        val journeyWithEoriToAdd = subsidyJourney
          .setClaimEori(OptionalClaimEori("true", eori3.some, addToUndertaking = true))
          .setAddBusiness(true)
          .copy(traderRef = TraderRefFormPage())

        "the user answers yes to adding the business" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd))
          }

          val result = performAction(
            "add-claim-business" -> "true"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimPublicAuthority.url)

        }

        "the user answers no to adding the business" in {
          inSequence {
            authAndSessionDataBehaviour.mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            journeyStoreSupport.mockGet[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.some))
            journeyStoreSupport.mockUpdate[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.setAddBusiness(false)))
          }

          val result = performAction(
            "add-claim-business" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)

        }
      }
    }

  }
}

object SubsidyControllerSpec {
  case class RemoveSubsidyRow(key: String, value: String)
}
