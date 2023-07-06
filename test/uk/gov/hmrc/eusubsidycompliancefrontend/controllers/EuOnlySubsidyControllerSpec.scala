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
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.LocalDate

class EuOnlySubsidyControllerSpec
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
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  private val controller = instanceOf[SubsidyController]
  private val exception = new Exception("oh no!")
  private val currentDate = LocalDate.of(2022, 10, 9)

  private val dateRange = (LocalDate.of(2020, 4, 6), LocalDate.of(2022, 10, 9))

  override def additionalConfig: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         | appName = "appName"
         | features.euro-only-enabled = "true"
         |
         |""".stripMargin
    )
  )

  "SubsidyControllerSpec" when {

    "handling request to get claim amount" must {

      def performAction = controller
        .getClaimAmount(FakeRequest(GET, routes.SubsidyController.getClaimAmount.url))

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "call to get subsidy journey fails " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }

      }
      "redirect" when {
        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))

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
          val journey = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          )

          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(journey.some))

          val result = performAction
          val document = Jsoup.parse(contentAsString(result))

          verifyCommonElements(document)

          document.getElementById(claimAmountEurosId).attr("value") shouldBe ""

        }

        "user has already entered a EUR amount" in {
          val journey = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
            claimAmount = ClaimAmountFormPage(value = claimAmountEuros.some)
          )

          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(journey.some))

          val result = performAction
          val document = Jsoup.parse(contentAsString(result))

          verifyCommonElements(document)

          document.getElementById(claimAmountEurosId).attr("value") shouldBe "123.45"
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back empty " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back with No claim date " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display form error" when {

        def testFormValidation(data: (String, String)*)(errorMessageKey: String): Unit = {
          val subsidyJourneyOpt = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          ).some
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
          }
          val errorMessage = messageFromMessageKey(errorMessageKey)

          val result = performAction(data: _*)

          val document = Jsoup.parse(contentAsString(result))

          verifyCommonElements(document, true)

          document.select(".govuk-error-summary").select("a").text() shouldBe errorMessage
          document.select(".govuk-error-message").text() shouldBe s"Error: $errorMessage"
        }

        def testConvertedAmountValidation(data: (String, String)*)(errorMessageKey: String): Unit = {

          val subsidyJourneyOpt = SubsidyJourney(
            claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
          ).some
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
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

        "nothing is entered" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.$Required")
        }

        "claim amount entered in EUR is too big" in {
          testFormValidation(
            ClaimAmountFormProvider.Fields.CurrencyCode -> EUR.entryName,
            ClaimAmountFormProvider.Fields.ClaimAmountEUR -> "€123456789012.12"
          )(s"add-claim-amount.claim-amount-${EUR.entryName.toLowerCase}.$TooBig")
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
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
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

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

  }

  private def verifyCommonElements(document: Document, errorPresent: Boolean = false) = {
    val titlePrefix = if (errorPresent) "Error: " else ""
    document.title shouldBe s"${titlePrefix}What is the payment amount in euros? - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
    document.getElementById("add-claim-amount-h1").text() shouldBe "What is the payment amount in euros?"
    document
      .getElementById("currency-exchange-link")
      .text() shouldBe "Convert payments in pounds to euros with this tool (opens in another tab)."
    document
      .getElementById("currency-exchange-link")
      .attr(
        "href"
      ) shouldBe "https://ec.europa.eu/info/funding-tenders/procedures-guidelines-tenders/information-contractors-and-beneficiaries/exchange-rate-inforeuro_en"
    document.getElementById("continue").text() shouldBe "Save and continue"

    document.getElementById("claim-amount-gbp") shouldBe null

  }
}
