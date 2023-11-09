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
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Result
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
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{TaxYearHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps

import java.time.LocalDate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

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
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[ExchangeRateService].toInstance(mockExchangeRateService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  private val controller = instanceOf[SubsidyController]
  private val exception = new Exception("oh no!")
  private val currentDate = LocalDate.of(2022, 10, 9)

  private val dateRange = (LocalDate.of(2020, 4, 6), LocalDate.of(2022, 10, 9))

  private val accentedCharactersList = List(
    "Áá Éé Íí Óó Úú Ññ Üü",
    "Àà Èè Ìì Òò Ùù Ââ Êê Îî Ôô Ûû",
    "Ää Öö Åå Ææ Øø",
    "Çç Şş Ğğ İı",
    "Ññ ß Ýý Ææ Œœ",
    "Šš Žž Čč Đđ",
    "Āā Ēē Īī Ōō Ūū",
    "ẞß Ẁẁ Ỳỳ Ẃẃ Ǣǣ",
    "Ǖǖ Ǘǘ Ǎǎ ǫǬ ǭ",
    "Ḉḉ Ḑḑ Ḕḕ Ḗḗ"
  )

  override def additionalConfig: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         | appName = "appName"
         | features.euro-only-enabled = "false"
         |
         |""".stripMargin
    )
  )

  "SubsidyControllerSpec" when {

    "handling request to get reported payments page" must {

      def performAction =
        controller.getReportedPayments(FakeRequest(GET, routes.SubsidyController.getReportedPayments.url))

      "throw technical error" when {
        "call to get undertaking from EIS fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(Future.failed(new RuntimeException("Oh no!")))
          }

          assertThrows[Exception](await(performAction))
        }

        "call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))

        }

      }

      "display the page" when {

        "Undertaking add claim date hint is available in" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeProviderToday(fixedDate)
          }
          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val findHintText = document.getElementById("claim-date-hint").text()
          val year = LocalDate.now().getYear - 1
          findHintText shouldBe s"For example, 15 3 ${year}"
        }

        "legend is displayed as H1 in fieldset component" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeProviderToday(fixedDate)
          }
          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val legendText: String = document.getElementsByClass("govuk-fieldset__heading").text()
          legendText shouldBe "What date were you awarded the payment?"
        }

        "a valid request is made" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeProviderToday(fixedDate)
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-date.title"),
            { doc =>
              val legend = doc.getElementsByClass("govuk-fieldset__heading")
              legend.size() shouldBe 1
              legend.first().text() shouldBe messageFromMessageKey("add-claim-date.p1")

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
          testLeadOnlyRedirect(() => performAction, checkSubmitted = true)
        }

      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
      }
    }

    "handling request to post claim date" must {

      def performAction(data: (String, String)*) = controller.postClaimDate(
        FakeRequest(POST, routes.SubsidyController.postClaimDate.url).withFormUrlEncodedBody(data: _*)
      )

      "return to claims page with Error Message" when {
        "entered date is not valid" in {
          val updatedDate = DateFormValues("21", "20", "2021")
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockTimeProviderToday(fixedDate)
          }
          val result = performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year)
          val document = Jsoup.parse(contentAsString(result))

          status(result) shouldBe BAD_REQUEST
          val findElement = document.getElementById("claim-date-error").text()
          findElement shouldBe "Error: " + messageFromMessageKey("add-claim-date.error.incorrect-format")

        }

      }

      "redirect to the next page" when {
        "entered date is valid" in {
          val updatedDate = DateFormValues("1", "2", LocalDate.now().getYear.toString)
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](eori1)(
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockTimeProviderToday(fixedDate)
          }
          status(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year)
          ) shouldBe BAD_REQUEST
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
      }
    }

    "handling submit report request to get first time user page" must {

      def performAction = controller.getReportPaymentFirstTimeUser(
        FakeRequest(GET, routes.SubsidyController.getReportPaymentFirstTimeUser.url)
      )

      "call to get session fails" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
        }
        assertThrows[Exception](await(performAction))
      }

      "display the page" when {
        "a request is made" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
            mockTimeProviderToday(fixedDate)
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("reportPaymentFirstTimeUser.title", "6 April 2018"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url
              doc.select("form").attr("action") shouldBe routes.SubsidyController.postReportPaymentFirstTimeUser.url
            }
          )
        }
      }

    }

    "handling post to report payment first time user page" must {
      def performAction(form: (String, String)*) = controller.postReportPaymentFirstTimeUser(
        FakeRequest(POST, routes.SubsidyController.postReportPaymentFirstTimeUser.url)
          .withFormUrlEncodedBody(form: _*)
      )

      "redirect to next page" when {

        val journeyWithReportPaymentFirstTimeUser = subsidyJourney
          .setReportPaymentFirstTimeUser(true)

        "the user selects 'true' for report payment first time user" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(fixedDate)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithReportPaymentFirstTimeUser.some))
            mockUpdate[SubsidyJourney](eori1)(Right(journeyWithReportPaymentFirstTimeUser))

          }

          val result = performAction(
            "reportPaymentFirstTimeUser" -> "true"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getReportedNoCustomSubsidyPage.url)
        }

        "the user selects 'false' for report payment first time user" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(fixedDate)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithReportPaymentFirstTimeUser.some))
            mockUpdate[SubsidyJourney](eori1)(
              Right(journeyWithReportPaymentFirstTimeUser.setReportPaymentFirstTimeUser(false))
            )
          }

          val result = performAction(
            "reportPaymentFirstTimeUser" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.NoClaimNotificationController.getNoClaimNotification.url)
        }
      }

      "return bad request" when {
        "the form submission is invalid" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(fixedDate)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }

          val result = performAction()
          checkFormErrorIsDisplayed(
            result,
            messageFromMessageKey(
              "reportPaymentFirstTimeUser.title",
              "6 April 2018"
            ),
            messageFromMessageKey(
              "reportPaymentFirstTimeUser.error.required"
            )
          )
        }
      }

      "show form error" when {

        def performAction(form: (String, String)*) = controller.postReportPaymentFirstTimeUser(
          FakeRequest(POST, routes.SubsidyController.getReportPaymentFirstTimeUser.url)
            .withFormUrlEncodedBody(form: _*)
        )

        def testFormError(
          input: Option[List[(String, String)]],
          errorMessageKey: String
        ): Unit = {
          val radio = input.getOrElse(Nil)
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(fixedDate)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }

          checkFormErrorIsDisplayed(
            performAction(radio: _*),
            messageFromMessageKey("reportPaymentFirstTimeUser.title", "6 April 2018"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "reportPaymentFirstTimeUser.error.required")
        }
      }
    }

    "handling post to reported payment returning user page" must {
      def performAction(form: (String, String)*) = controller.postReportedPaymentReturningUserPage(
        FakeRequest(POST, routes.SubsidyController.postReportedPaymentReturningUserPage.url)
          .withFormUrlEncodedBody(form: _*)
      )

      "redirect to next page" when {
        val journeyWithReportedPaymentReturningUser = subsidyJourney
          .setReportedPaymentReturningUser(true)

        "the user selects 'true' for reported payment returning user" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithReportedPaymentReturningUser.some))
            mockUpdate[SubsidyJourney](eori1)(Right(journeyWithReportedPaymentReturningUser))
          }

          val result = performAction(
            "report-payment-return" -> "true"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getReportedNoCustomSubsidyPage.url)
        }

        "the user selects 'false' for reported payment returning user" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithReportedPaymentReturningUser.some))
            mockUpdate[SubsidyJourney](eori1)(
              Right(journeyWithReportedPaymentReturningUser.setReportedPaymentReturningUser(false))
            )
          }

          val result = performAction(
            "report-payment-return" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.NoClaimNotificationController.getNoClaimNotification.url)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
      }
    }

    "handling request to get returning user page" must {

      def performAction = controller.getReportedPaymentReturningUserPage(
        FakeRequest(GET, routes.SubsidyController.getReportedPaymentReturningUserPage.url)
      )

      "call to get session fails" in {
        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
        }
        assertThrows[Exception](await(performAction))
      }

      "display the page" when {
        "a request is made" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("reportPaymentReturnJourney.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url
              doc
                .select("form")
                .attr("action") shouldBe routes.SubsidyController.postReportedPaymentReturningUserPage.url
            }
          )
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
      }
    }

    "handling get to reported all non custom subsidy page" must {
      def performAction(): Future[Result] = controller.getReportedNoCustomSubsidyPage(FakeRequest())

      "render the page with bullet list of current tax year and previous 2 years for returning user" in {
        val journeyWithReportedNonCustomSubsidyReturningUser = subsidyJourney
          .setReportedPaymentReturningUser(true)

        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[SubsidyJourney](eori1)(Right(journeyWithReportedNonCustomSubsidyReturningUser))
        }

        val result = performAction()
        val document: Document = Jsoup.parse(contentAsString(result))
        document
          .select(".govuk-back-link")
          .attr("href") shouldBe routes.SubsidyController.getReportedPaymentReturningUserPage.url

        verifyTaxYearsPage(result, document)

      }

      "render the page with bullet list of current tax year and previous 2 years for first time user" in {
        val journeyWithReportedNonCustomSubsidyFirstTimeUser = subsidyJourney
          .setReportedPaymentReturningUser(false)

        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[SubsidyJourney](eori1)(Right(journeyWithReportedNonCustomSubsidyFirstTimeUser))
        }

        val result = performAction()
        val document: Document = Jsoup.parse(contentAsString(result))

        document
          .select(".govuk-back-link")
          .attr("href") shouldBe routes.SubsidyController.getReportPaymentFirstTimeUser.url

        verifyTaxYearsPage(result, document)
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }
      "redirect" when {
        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          redirectLocation(performAction) shouldBe Some(routes.SubsidyController.getClaimDate.url)
        }
      }

      "display the page" when {

        val claimAmountEurosId = "claim-amount-eur"
        val claimAmountPoundsId = "claim-amount-gbp"

        def test(subsidyJourney: SubsidyJourney, elementId: String): Unit = {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          mockRetrieveExchangeRate(claimDate2)(Some(exchangeRate).toFuture)

          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-amount.title"),
            { doc =>
              val input = doc.getElementById(elementId).attributes().get("value")
              input shouldBe subsidyJourney.claimAmount.value.map(_.amount).getOrElse("")

              doc.getElementById("euros-label").text() shouldBe messageFromMessageKey(
                "add-claim-amount.euros.input-label"
              )
              doc.getElementById("pounds-label").text() shouldBe messageFromMessageKey(
                "add-claim-amount.pounds.input-label"
              )

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
          testLeadOnlyRedirect(() => performAction, checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back empty " in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back with No claim date " in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the subsidy journey fails" in {

          val subsidyJourney = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
          )

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
            mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
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
            claimDate = ClaimDateFormPage(
              DateFormValues(
                claimDate.getDayOfMonth.toString,
                claimDate.getMonthValue.toString,
                claimDate.getYear.toString
              ).some
            )
          ).some
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
            mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
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
            claimDate = ClaimDateFormPage(
              DateFormValues(
                claimDate.getDayOfMonth.toString,
                claimDate.getMonthValue.toString,
                claimDate.getYear.toString
              ).some
            )
          ).some
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
            mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
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

        "claim amount entered in GBP is in wrong format" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> GBP.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "123.4"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.$IncorrectFormat")
        }

        "claim amount entered in EUR is in wrong format" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "123.4"
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.$IncorrectFormat")
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
            ClaimAmountFormProvider.Fields.ClaimAmountGBP -> "£999999999999.99"
          )(s"add-claim-amount.claim-amount-${GBP.entryName.toLowerCase}.$TooBig")
        }

      }

      "redirect to the add claim eori page if an EUR amount is entered" in {
        val subsidyJourney = SubsidyJourney(
          claimDate = ClaimDateFormPage(
            DateFormValues(
              claimDate.getDayOfMonth.toString,
              claimDate.getMonthValue.toString,
              claimDate.getYear.toString
            ).some
          )
        )

        val claimAmount = "100.00"

        val subsidyJourneyWithClaimAmount = subsidyJourney.copy(
          claimAmount = ClaimAmountFormPage(ClaimAmount(EUR, claimAmount).some)
        )

        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
          mockUpdate[SubsidyJourney](eori1)(Right(subsidyJourneyWithClaimAmount))
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
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
          mockUpdate[SubsidyJourney](eori1)(Right(subsidyJourney))
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
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(
              Right(subsidyJourney.copy(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).some)
            )
            mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
          }

          checkPageIsDisplayed(
            result = performAction(),
            expectedTitle = messageFromMessageKey("confirm-converted-amount.title")
          )

        }

      }

      "redirect" when {

        "user is not an undertaking lead, to the account home page" in {
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }

      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction()))
        }

        "the call to retrieve the exchange rate fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(initialJourney.some))
            mockRetrieveExchangeRate(claimDate)(Some(exchangeRate).toFuture)
            mockPut[SubsidyJourney](updatedJourney, eori1)(Right(updatedJourney))
          }

          val result = performAction()
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)
        }

      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
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
                  inputText shouldBe eori.getOrElse("")
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
          testLeadOnlyRedirect(() => performAction, checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update subsidy journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
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
          testFormError(Some(List("should-claim-eori" -> "true")), s"claim-eori.$Required")

        }

        "yes is selected but eori entered is invalid" in {
          testFormError(
            Some(List("should-claim-eori" -> "true", "claim-eori" -> "GBUK1234567890")),
            s"claim-eori.$IncorrectFormat"
          )

        }

        "yes is selected but eori entered is part of another undertaking" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journey.some))
            mockUpdate[SubsidyJourney](eori1)(Right(updatedSubsidyJourney))
          }
          checkIsRedirect(performAction(inputAnswer: _*), routes.SubsidyController.getAddClaimPublicAuthority.url)
        }

        "user selected yes and entered a valid eori part of the existing undertaking" in {
          testRedirect(
            OptionalClaimEori("true", "GB123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "GB123456789013")
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journey.some))
            mockRetrieveUndertaking(eori3)(Option.empty.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Right(updatedSubsidyJourney))
          }

          checkIsRedirect(
            performAction("should-claim-eori" -> optionalEORI.setValue, "claim-eori" -> eori3),
            routes.SubsidyController.getAddClaimBusiness.url
          )
        }

        "user selected yes and entered a valid EORI that is not part of the existing or any other undertaking and has spaces" in {
          val eori = "GB 99 34 56 78 90 99"
          val optionalEORI = OptionalClaimEori("true", eori.some)

          val updatedSubsidyJourney = update(journey, optionalEORI.copy(addToUndertaking = true).some)

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journey.some))
            mockRetrieveUndertaking(EORI(eori.replaceAll(" ", "")))(Option.empty.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Right(updatedSubsidyJourney))
          }

          checkIsRedirect(
            performAction("should-claim-eori" -> optionalEORI.setValue, "claim-eori" -> eori),
            routes.SubsidyController.getAddClaimBusiness.url
          )
        }

        "user selected no " in {
          testRedirect(OptionalClaimEori("false", None), List("should-claim-eori" -> "false"))
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(incompleteJourney))
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("add-claim-public-authority.title"),
            { doc =>
              doc
                .getElementById("add-claim-public-authority.title")
                .text shouldBe "Public authorities and subsidy payments"
              doc
                .getElementById("add-claim-public-authority.p1")
                .text shouldBe "Your subsidy payment may have come from:"
              doc
                .getElementById("add-claim-public-authority.p2")
                .text shouldBe "a government department, like the Department of Business & Trade"
              doc
                .getElementById("add-claim-public-authority.p3")
                .text shouldBe "a local government authority, like the London Borough of Hounslow"
              doc
                .getElementById("add-claim-public-authority.p4")
                .text shouldBe "other public authorities and offices, like the British Council"
              doc.select("#claim-public-authority-hint").text() shouldBe "For example Invest NI, NI Direct"
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimPublicAuthority.url
            }
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction, checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journey.some))
            mockUpdate[SubsidyJourney](eori1)(
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
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
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
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
          testLeadOnlyRedirect(() => performAction, checkSubmitted = true)
        }

      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction, routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
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

      "redirect to the next page" when {
        "user selected No" in {
          mockForPostAddTraderReference
          checkIsRedirect(
            performAction("should-store-trader-ref" -> "false"),
            routes.SubsidyController.getCheckAnswers.url
          )
        }

        "user selected Yes and enters valid reference format" in {
          mockForPostAddTraderReference
          checkIsRedirect(
            performAction("should-store-trader-ref" -> "true", "claim-trader-ref" -> "valid ref"),
            routes.SubsidyController.getCheckAnswers.url
          )
        }

        "user selected Yes and enters valid reference format with more than 36 characters" in {
          mockForPostAddTraderReference
          checkIsRedirect(
            performAction(
              "should-store-trader-ref" -> "true",
              "claim-trader-ref" -> "1234567890 1234567890 1234567890 1234567890"
            ),
            routes.SubsidyController.getCheckAnswers.url
          )
        }
        "user selected Yes and enters Valid reference including special characters" in {
          mockForPostAddTraderReference
          checkIsRedirect(
            performAction(
              "should-store-trader-ref" -> "true",
              "claim-trader-ref" -> "Test-Reference_with!Special@Characters"
            ),
            routes.SubsidyController.getCheckAnswers.url
          )
        }

        "user selected Yes and enters Valid reference including characters with accents" in {
          accentedCharactersList.map(currentString => {
            mockForPostAddTraderReference
            checkIsRedirect(
              performAction(
                "should-store-trader-ref" -> "true",
                "claim-trader-ref" -> currentString
              ),
              routes.SubsidyController.getCheckAnswers.url
            )
          })
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy failed" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy comes back with nonHMRC subsidy usage list with missing transactionId" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
          mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to remove subsidy fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidiesForDateRange(undertakingRef, dateRange)(undertakingSubsidies1.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()("TID1234"),
            "You are about to remove a reported payment",
            "Select yes if you want to remove this reported payment"
          )
        }
      }

      "display reported payments page with successful removal banner" when {

        "If user select yes" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
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

      "redirect to account home if no subsidy journey data found" in {
        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(Option.empty))
        }

        val result = performAction()

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
      }

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
        }

        status(performAction()) shouldBe OK
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to create subsidy fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Right(updatedJourney))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to delete subsidy journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Right(updatedJourney))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Right(undertakingRef))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }
      }

      "redirect to next page" when {

        "cya page is reached via add journey" in {

          val updatedSJ = subsidyJourney.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdate[SubsidyJourney](eori1)(Right(updatedSJ))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
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
          testLeadOnlyRedirect(() => performAction(), checkSubmitted = true)
        }
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedSubsidyJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.PaymentSubmittedController.paymentAlreadySubmitted.url)
        }
      }
    }

    "handling get of claim confirmation" must {
      def performAction() = controller.getClaimConfirmationPage(
        FakeRequest(GET, routes.SubsidyController.getClaimConfirmationPage.url)
      )

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockTimeProviderToday(fixedDate)
        }

        val result = performAction()

        status(result) shouldBe OK

        val expectedDeadlineDate = "20 April 2021"
        contentAsString(result) should include(expectedDeadlineDate)

        val document = Jsoup.parse(contentAsString(result))
        document
          .getElementById("report-another-payment")
          .attr("href") shouldBe routes.SubsidyController.startJourney.url
        document.getElementById("home-link").attr("href") shouldBe routes.AccountController.getAccountPage.url

      }

      "title, heading and paragraph body available on getClaimConfirmation Page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockTimeProviderToday(fixedDate)
        }
        val result = performAction
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))

        val claimTitle = document.getElementsByClass("govuk-panel__title").text()
        claimTitle shouldBe "Payment reported"
        val headingTitle = document.getElementById("claimSubheadingId").text()
        headingTitle shouldBe "What happens next"
        val paragraph = document.getElementById("claimParaId").text()
        paragraph shouldBe "Your next report must be made by 20 April 2021."
      }

      "Display hyperlinks on getClaimConfirmation page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockTimeProviderToday(fixedDate)
        }
        val result = performAction
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))

        val reportPaymentHref = document.getElementById("report-another-payment").attr("href")
        val homeHref = document.getElementById("home-link").attr("href")
        reportPaymentHref shouldBe "/report-and-manage-your-allowance-for-customs-duty-waiver-claims/lead-undertaking-returning-user/start"
        homeHref shouldBe "/report-and-manage-your-allowance-for-customs-duty-waiver-claims"
      }

      "redirect to payment already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockTimeProviderToday(fixedDate)
          }

          checkPageIsDisplayed(
            performAction,
            "Payment reported"
          )
        }
      }

    }

    "handling get of add claim business page" must {
      def performAction() = controller.getAddClaimBusiness(
        FakeRequest(GET, routes.SubsidyController.getClaimConfirmationPage.url)
      )

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.some))
            mockUpdate[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd))
          }

          val result = performAction(
            "add-claim-business" -> "true"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimPublicAuthority.url)

        }

        "the user answers no to adding the business" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.some))
            mockUpdate[SubsidyJourney](eori1)(Right(journeyWithEoriToAdd.setAddBusiness(false)))
          }

          val result = performAction(
            "add-claim-business" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)

        }
      }
    }

    "startJourney" must {
      def performAction() = controller.startJourney(
        FakeRequest(GET, routes.SubsidyController.startJourney.url)
      )

      "redirect to returning user page" when {
        "user starts new journey" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
          }

          val result = performAction()

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getReportedPaymentReturningUserPage.url)

        }
      }
    }

    "startFirstTimeUserJourney" must {
      def performAction() = controller.startFirstTimeUserJourney(
        FakeRequest(GET, "/some-url/")
      )

      "redirect to first time user page" when {
        "user starts new journey" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
          }

          val result = performAction()

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.SubsidyController.getReportPaymentFirstTimeUser.url)

        }
      }
    }

  }

  private def verifyTaxYearsPage(result: Future[Result], document: Document) = {
    val currentDate = LocalDate.now
    val currentTaxYearStartYear = TaxYearHelpers.taxYearStartForDate(currentDate).getYear.toString
    val currentTaxYearEndYear = TaxYearHelpers.taxYearEndForDate(currentDate).getYear.toString
    val previousTaxYearStartYear = TaxYearHelpers.taxYearStartForDate(currentDate.minusYears(1)).getYear.toString
    val twoYeasAgoTaxYearStartYear = TaxYearHelpers.taxYearStartForDate(currentDate.minusYears(2)).getYear.toString
    val expectedTaxYears = List(
      s"6 April $twoYeasAgoTaxYearStartYear to 5 April $previousTaxYearStartYear",
      s"6 April $previousTaxYearStartYear to 5 April $currentTaxYearStartYear",
      s"6 April $currentTaxYearStartYear to 5 April $currentTaxYearEndYear"
    )

    status(result) shouldBe OK

    document.getElementById("twoYearBack").text shouldBe expectedTaxYears.head
    document.getElementById("previousYear").text shouldBe expectedTaxYears(1)
    document.getElementById("currentYear").text shouldBe expectedTaxYears(2)
  }

  private def mockForPostAddTraderReference = {
    inSequence {
      mockAuthWithEnrolmentWithValidEmailAndUnsubmittedSubsidyJourney()
      mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
      mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
      mockUpdate[SubsidyJourney](eori1)(
        Right(subsidyJourney)
      )
    }
  }
}

object SubsidyControllerSpec {
  case class RemoveSubsidyRow(key: String, value: String)
}
