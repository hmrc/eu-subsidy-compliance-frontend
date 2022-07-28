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
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyControllerSpec.RemoveSubsidyRow
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyRemoved
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyRef
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps

import java.time.LocalDate
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.Future

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
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  private val controller = instanceOf[SubsidyController]
  private val exception = new Exception("oh no!")
  private val currentDate = LocalDate.of(2022, 10, 9)

  private val subsidyRetrieveWithDates = subsidyRetrieve.copy(
    inDateRange = Some((LocalDate.of(2020, 4, 6), LocalDate.of(2022, 10, 9)))
  )

  "SubsidyControllerSpec" when {

    "handling request to get report payment page" must {

      def performAction() =
        controller.getReportPayment(FakeRequest("GET", routes.SubsidyController.getReportPayment().url))

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get undertaking from EIS fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(Future.failed(new RuntimeException("Oh no!")))
          }

          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        val previousUrl = routes.AccountController.getAccountPage().url

        def test(nonHMRCSubsidyUsage: List[NonHmrcSubsidy], subsidyJourney: SubsidyJourney) = {
          val subsidies = undertakingSubsidies.copy(nonHMRCSubsidyUsage = nonHMRCSubsidyUsage)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeToday(currentDate)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(subsidies.toFuture)
          }
        }

        "user hasn't already answered the question" in {
          test(List.empty, SubsidyJourney())
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("reportPayment.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              val button = doc.select("form")
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty shouldBe true
              button.attr("action") shouldBe routes.SubsidyController.postReportPayment().url
              doc.select("#subsidy-list").size() shouldBe 0
            }
          )
        }

        "user has already answered the question" in {
          test(
            nonHmrcSubsidyList.map(_.copy(subsidyUsageTransactionId = SubsidyRef("Z12345").some)),
            SubsidyJourney(reportPayment = ReportPaymentFormPage(true.some))
          )
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("reportPayment.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.attr("value") shouldBe "true"
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
              subsidyList.select("tbody > tr > td:nth-child(6)").text() shouldBe "Change payment, dated 1 Jan 2022"
              subsidyList.select("tbody > tr > td:nth-child(7)").text() shouldBe "Remove payment, dated 1 Jan 2022"

              subsidyList.select("tbody > tr > td:nth-child(6) > a").attr("href") shouldBe routes.SubsidyController
                .getChangeSubsidyClaim("Z12345")
                .url
              subsidyList.select("tbody > tr > td:nth-child(7) > a").attr("href") shouldBe routes.SubsidyController
                .getRemoveSubsidyClaim("Z12345")
                .url

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postReportPayment().url
            }
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post report payment" must {

      def performAction(data: (String, String)*) = controller.postReportPayment(
        FakeRequest("POST", routes.SubsidyController.postReportPayment().url).withFormUrlEncodedBody(data: _*)
      )

      "throw technical error" when {

        "call to update subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[SubsidyJourney](identity, eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("reportPayment" -> "true")))

        }
      }

      "display the form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies.toFuture)
            mockTimeToday(currentDate)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("reportPayment.title"),
            messageFromMessageKey("reportPayment.error.required")
          )
        }
      }

      "redirect to the next page" when {
        def testRedirection(input: String, nextCall: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[SubsidyJourney](identity, eori1)(Right(subsidyJourney))
          }
          checkIsRedirect(performAction(("reportPayment", input)), nextCall)
        }

        "user selected Yes" in {
          testRedirection("true", routes.SubsidyController.getClaimDate().url)
        }

        "user selects No" in {
          testRedirection("false", routes.AccountController.getAccountPage().url)
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling request to get claim date page" must {

      def performAction() = controller.getClaimDate(FakeRequest(GET, routes.SubsidyController.getClaimDate().url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get session fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" when {

        "a valid request is made" in {
          inAnyOrder {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-date.title"),
            { doc =>
              doc.select("#claim-date > div:nth-child(1) > div > label").text() shouldBe "Day"
              doc.select("#claim-date > div:nth-child(2) > div > label").text() shouldBe "Month"
              doc.select("#claim-date > div:nth-child(3) > div > label").text() shouldBe "Year"
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postClaimDate().url
            }
          )
        }

      }

      "redirect " when {

        "user is not an undertaking lead to account home page" in {
          testLeadOnlyRedirect(performAction)
        }

        "call to get sessions returns none, to subsidy start journey" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.SubsidyController.getReportPayment().url)

        }
      }
    }

    "handling request to post claim date" must {

      def performAction(data: (String, String)*) = controller.postClaimDate(
        FakeRequest("POST", routes.SubsidyController.postClaimDate().url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        "valid input" in {
          val updatedDate = DateFormValues("1", "2", LocalDate.now().getYear.toString)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](j => j.copy(claimDate = ClaimDateFormPage(updatedDate.some)), eori1)(
              Right(subsidyJourney.copy(claimDate = ClaimDateFormPage(updatedDate.some)))
            )
          }
          checkIsRedirect(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year),
            routes.SubsidyController.getClaimAmount().url
          )
        }
      }

      "invalid input" should {
        "invalid date" in {
          val updatedDate = DateFormValues("20", "20", LocalDate.now().getYear.toString)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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

      def performAction() = controller
        .getClaimAmount(FakeRequest("GET", routes.SubsidyController.getClaimAmount().url))

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to get subsidy journey fails " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }
      "redirect" when {
        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          redirectLocation(performAction()) shouldBe Some(routes.SubsidyController.getClaimDate().url)
        }
      }

      "display the page" when {

        val claimAmountEurosId = "claim-amount-eur"
        val claimAmountPoundsId = "claim-amount-gbp"

        def test(subsidyJourney: SubsidyJourney, elementId: String): Unit = {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-amount.title"),
            { doc =>
              val input = doc.getElementById(elementId).attributes().get("value")
              input shouldBe subsidyJourney.claimAmount.value.map(_.amount).getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimAmount().url

            }
          )
        }

        "user hasn't already answered the question - EUR input field should be empty" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
            ),
            claimAmountEurosId
          )
        }

        "user hasn't already answered the question - GBP input field should be empty" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
            ),
            claimAmountPoundsId
          )
        }

        "user has already entered a EUR amount" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
              claimAmount = ClaimAmountFormPage(value = claimAmountEuros.some)
            ),
            claimAmountEurosId
          )
        }

        "user has already entered a GBP amount" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
              claimAmount = ClaimAmountFormPage(value = claimAmountPounds.some)
            ),
            claimAmountPoundsId
          )
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post claim amount" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimAmount(
          FakeRequest("POST", routes.SubsidyController.getClaimAmount().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to get subsidy journey fails " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back empty " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back with No claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the subsidy journey fails" in {

          val subsidyJourney = SubsidyJourney(
            reportPayment = ReportPaymentFormPage(true.some),
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          )

          def update(subsidyJourney: SubsidyJourney) =
            subsidyJourney.copy(claimAmount = ClaimAmountFormPage(value = claimAmountPounds.some))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction(
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "123.45",
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
          )))
        }
      }

      "display form error" when {

        def displayError(data: (String, String)*)(errorMessageKey: String): Unit = {

          val subsidyJourneyOpt = SubsidyJourney(
            reportPayment = ReportPaymentFormPage(true.some),
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          ).some
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
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
          displayError()("add-claim-amount.currency-code.error.required")
        }

        "nothing is entered" in {
          CurrencyCode.values.foreach { c =>
            displayError(
              ClaimAmountFormProvider.Fields.CurrencyCode -> c.entryName,
            )(s"add-claim-amount.claim-amount-${c.entryName.toLowerCase}.error.required")
          }
        }

        "claim amount entered in wrong format" in {
          CurrencyCode.values.foreach { c =>
            displayError(
              ClaimAmountFormProvider.Fields.CurrencyCode -> c.entryName,
              ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "123.4"
            )(s"add-claim-amount.claim-amount-${c.entryName.toLowerCase}.error.incorrectFormat")
          }
        }

        "claim amount entered in GBP is more than 17 chars" in {
          displayError(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "1234567890.12345678",
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.error.tooBig")
        }

        "claim amount entered in EUR is more than 17 chars" in {
          displayError(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "1234567890.12345678",
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.error.tooBig")
        }

        "claim amount entered in GBP is too small" in {
          displayError(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "0.00"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.error.tooSmall")
        }

        "claim amount entered in EUR is too small" in {
          displayError(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "0.00"
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.error.tooSmall")
        }

      }

      "redirect to the add claim eori page if an EUR amount is entered" in {
        val subsidyJourney = SubsidyJourney(
          reportPayment = ReportPaymentFormPage(true.some),
          claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
        )

        val claimAmount = "100.00"

        val subsidyJourneyWithClaimAmount = subsidyJourney.copy(
          claimAmount = ClaimAmountFormPage(ClaimAmount(EUR, claimAmount).some)
        )

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          mockUpdate[SubsidyJourney](_ => subsidyJourneyWithClaimAmount, eori1)(Right(subsidyJourneyWithClaimAmount))
        }

        checkIsRedirect(
          performAction(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> claimAmount
          ),
          routes.SubsidyController.getAddClaimEori().url
        )
    }

    "redirect to the confirm converted amount page if a GBP amount is entered" in {
      val subsidyJourney = SubsidyJourney(
        reportPayment = ReportPaymentFormPage(true.some),
        claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
      )

      val claimAmount = "100.00"

      val subsidyJourneyWithClaimAmount = subsidyJourney.copy(
        claimAmount = ClaimAmountFormPage(ClaimAmount(GBP, claimAmount).some)
      )

      inSequence {
        mockAuthWithNecessaryEnrolment()
        mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
        mockUpdate[SubsidyJourney](_ => subsidyJourneyWithClaimAmount, eori1)(Right(subsidyJourney))
      }

      checkIsRedirect(
        performAction(
          ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
          ClaimAmountFormProvider.Fields.ClaimAmountGBP -> claimAmount
        ),
        routes.SubsidyController.getConfirmClaimAmount().url
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
        FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount().url)
      )

      "throw a technical error" when {

        "the call to fetch the subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some))
            mockRetrieveExchangeRate(claimDate)(Future.failed(exception))
          }

          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "a successful request is made" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some))
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
          }

          checkPageIsDisplayed(
            result = performAction(),
            expectedTitle = messageFromMessageKey("confirm-converted-amount.title"),
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
        FakeRequest(POST, routes.SubsidyController.postConfirmClaimAmount().url)
      )

      "throw technical error" when {

        "the call to fetch the subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some))
            mockRetrieveExchangeRate(claimDate)(Future.failed(exception))
          }

          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect to next page" when {

        "the user submits the form to accept the exchange rate" in {
          val subsidyJourneyWithPoundsAmount = subsidyJourney.copy(
            claimAmount = ClaimAmountFormPage(claimAmountPounds.some),
            convertedClaimAmountConfirmation = ConvertedClaimAmountConfirmationPage(ClaimAmount(EUR, "138.55").some)
          )

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some))
            mockRetrieveExchangeRate(claimDate)(exchangeRate.toFuture)
            mockPut[SubsidyJourney](subsidyJourneyWithPoundsAmount, eori1)(Right(subsidyJourneyWithPoundsAmount))
          }

          val result = performAction()
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori().url)
        }

      }

    }

    "handling request to get Add Claim Eori" must {

      def performAction() = controller
        .getAddClaimEori(FakeRequest("GET", routes.SubsidyController.getAddClaimEori().url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "the call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-eori.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.addClaimEori.value match {
                case Some(OptionalEORI(input, eori)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe eori.map(_.drop(2)).getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimEori().url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(None)))
        }

        "the user has already answered the question" in {
          List(
            subsidyJourney,
            subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(OptionalEORI("false", None).some))
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
          testLeadOnlyRedirect(performAction)
        }

        "the call to get subsidy journey comes back empty, to subsidy start journey page" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.SubsidyController.getReportPayment().url)
        }
      }
    }

    "handling the request to post add claim eori" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimEori(
          FakeRequest("POST", routes.SubsidyController.getAddClaimEori().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update subsidy journey fails" in {

          def update(subsidyJourney: SubsidyJourney) =
            subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(OptionalEORI("false", None).some))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(None))),
              eori1
            )(Left(ConnectorError(exception)))
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
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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
          testFormError(Some(List("should-claim-eori" -> "true")), "claim-eori.error.required")

        }

        "yes is selected but eori entered is invalid" in {
          testFormError(
            Some(List("should-claim-eori" -> "true", "claim-eori" -> "GB1234567890")),
            "claim-eori.error.format"
          )

        }

        "yes is selected but the eori entered is not part of the undertaking" in {
          testFormError(
            Some(List("should-claim-eori" -> "true", "claim-eori" -> "121212121212")),
            "claim-eori.error.not-in-undertaking"
          )
        }

      }

      "redirect to next page" when {

        def update(subsidyJourney: SubsidyJourney, formValues: Option[OptionalEORI]) =
          subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(formValues))

        def testRedirect(optionalEORI: OptionalEORI, inputAnswer: List[(String, String)]): Unit = {
          val updatedSubsidyJourney = update(subsidyJourney, optionalEORI.some)

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney, optionalEORI.some), eori1)(
              Right(updatedSubsidyJourney)
            )
          }
          checkIsRedirect(performAction(inputAnswer: _*), routes.SubsidyController.getAddClaimPublicAuthority().url)
        }

        "user selected yes and enter a valid  eori number" in {
          testRedirect(
            OptionalEORI("true", "123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "123456789013")
          )
        }

        "user selected yes and enter a valid eori number with GB" in {
          testRedirect(
            OptionalEORI("true", "GB123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "GB123456789013")
          )
        }

        "user selected No " in {
          testRedirect(OptionalEORI("false", None), List("should-claim-eori" -> "false"))
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get add claim public authority" must {

      def performAction() = controller.getAddClaimPublicAuthority(FakeRequest(
        GET,
        routes.SubsidyController.getAddClaimPublicAuthority().url
      ))

      "display the page" when {
        "happy path" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-public-authority.title"),
            { doc =>
              doc.select("#claim-public-authority-hint").text() shouldBe "For example, Invest NI, NI Direct."
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimPublicAuthority().url
            }
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }
    }

    "handling request to post add claim public authority" must {

      def performAction(data: (String, String)*) = controller.postAddClaimPublicAuthority(
        FakeRequest("POST", routes.SubsidyController.postAddClaimPublicAuthority().url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        "valid input" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](
              j => j.copy(publicAuthority = PublicAuthorityFormPage(Some("My Authority"))),
              eori1
            )(
              Right(subsidyJourney.copy(publicAuthority = PublicAuthorityFormPage(Some("My Authority"))))
            )
          }
          checkIsRedirect(
            performAction("claim-public-authority" -> "My Authority"),
            routes.SubsidyController.getAddClaimReference().url
          )
        }
      }

      "show form error" when {

        def displayError(data: (String, String)*)(messageKey: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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

        "public authority entered is more than 150 chars" in {
          displayError("claim-public-authority" -> "x" * 151)("error.claim-public-authority.tooManyChars")
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling request to get Add Claim Reference" must {
      def performAction() = controller
        .getAddClaimReference(FakeRequest("GET", routes.SubsidyController.getAddClaimReference().url))

      "throw technical error" when {

        val exception = new Exception("oh no")

        "the call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-trader-reference.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.traderRef.value match {
                case Some(OptionalStringFormInput(input, traderRef)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe traderRef.getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimReference().url
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
            subsidyJourney.copy(traderRef = TraderRefFormPage(OptionalStringFormInput("false", None).some))
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
          testLeadOnlyRedirect(performAction)
        }

        "the call to get subsidy journey comes back empty, to Start of the journey" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.SubsidyController.getReportPayment().url)
        }
      }

    }

    "handling request to post Add Claim Reference" must {
      def performAction(data: (String, String)*) = controller
        .postAddClaimReference(
          FakeRequest("POST", routes.SubsidyController.getAddClaimReference().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "show form error" when {

        def testFormError(inputAnswer: Option[List[(String, String)]], errorMessageKey: String): Unit = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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
          FakeRequest("GET", routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
        )

      "throw technical error" when {

        "call to fetch undertaking passes but come back with empty reference" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy failed" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy comes back with nonHMRC subsidy usage list with missing transactionId" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies.toFuture)
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
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeToday(currentDate)
          mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
        }

        checkPageIsDisplayed(
          performAction(transactionId),
          messageFromMessageKey("subsidy.remove.title"),
          { doc =>
            val rows =
              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
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
          FakeRequest("POST", routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        "call to retrieve subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to remove subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

      }

      "display page error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()("TID1234"),
            messageFromMessageKey("subsidy.remove.title"),
            messageFromMessageKey("subsidy.remove.error.required")
          )
        }
      }

      "redirect to next page" when {

        "If user select yes" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Right(undertakingRef))
            mockSendAuditEvent[NonCustomsSubsidyRemoved](AuditEvent.NonCustomsSubsidyRemoved("1123", undertakingRef))
          }
          checkIsRedirect(
            performAction("removeSubsidyClaim" -> "true")("TID1234"),
            routes.SubsidyController.getReportPayment().url
          )

        }

        "if user selects no" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeSubsidyClaim" -> "false")("TID1234"),
            routes.SubsidyController.getReportPayment().url
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
        FakeRequest("GET", routes.SubsidyController.getCheckAnswers().url)
      )

      "throw technical error" when {

        def testErrorFor(s: SubsidyJourney) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(s.some))
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

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
        }

        status(performAction()) shouldBe OK
      }

    }

    "handling post to check your answers" must {

      def performAction(data: (String, String)*) = controller
        .postCheckAnswers(
          FakeRequest("POST", routes.SubsidyController.getCheckAnswers().url)
            .withFormUrlEncodedBody(data: _*)
        )

      def update(subsidyJourney: SubsidyJourney) =
        subsidyJourney.copy(cya = CyaFormPage(value = true.some))

      val updatedJourney = subsidyJourney.copy(cya = CyaFormPage(value = true.some))

      "throw technical error" when {

        "call to update subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }


        "call to create subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney), eori1)(Right(updatedJourney))
            mockTimeToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to reset subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney), eori1)(Right(updatedJourney))
            mockTimeToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }
      }

      "redirect to next page" when {

        "cya page is reached via update journey" in {

          val subsidyJourneyExisting = subsidyJourney.copy(existingTransactionId = SubsidyRef("TD123").some)
          val updatedSJ = subsidyJourneyExisting.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourneyExisting),
              eori1
            )(Right(updatedSJ))
            mockTimeToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
            mockSendAuditEvent[AuditEvent.NonCustomsSubsidyUpdated](
              AuditEvent.NonCustomsSubsidyUpdated(
                ggDetails = "1123",
                undertakingRef = undertakingRef,
                subsidyJourney = updatedSJ,
                currentDate
              )
            )
          }

          checkIsRedirect(
            performAction("cya" -> "true"),
            routes.SubsidyController.getReportPayment().url
          )
        }

        "cya page is reached via add journey" in {

          val updatedSJ = subsidyJourney.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourney),
              eori1
            )(Right(updatedSJ))
            mockTimeToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
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
            routes.SubsidyController.getReportPayment().url
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "handling request to get change subsidy" should {

      val transactionID = "TID1234"

      def performAction() = controller.getChangeSubsidyClaim(transactionID)(
        FakeRequest("GET", routes.SubsidyController.getChangeSubsidyClaim(transactionID).url)
      )

      val subsidyJourneyWithReportPaymentForm =
        subsidyJourney.copy(
          reportPayment = ReportPaymentFormPage(Some(true)),
          claimAmount = ClaimAmountFormPage(ClaimAmount(EUR, nonHmrcSubsidyAmount.toString).some),
          existingTransactionId = Some(SubsidyRef(transactionID))
        )

      "throw technical error" when {

        "esc service returns an error retrieving subsidies" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))
        }

        "store returns an error when attempting to store subsidy journey" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
            mockPut[SubsidyJourney](subsidyJourneyWithReportPaymentForm, eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect to check answers page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          mockTimeToday(currentDate)
          mockRetrieveSubsidy(subsidyRetrieveWithDates)(undertakingSubsidies1.toFuture)
          mockPut[SubsidyJourney](subsidyJourneyWithReportPaymentForm, eori1)(
            Right(subsidyJourneyWithReportPaymentForm)
          )
        }

        val result = performAction()
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers().url)
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

  }
}

object SubsidyControllerSpec {
  case class RemoveSubsidyRow(key: String, value: String)
}
