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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, NonHmrcSubsidy, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingStatus

import java.time.LocalDate
import scala.concurrent.Future

class AccountControllerWithSCP08Spec
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
        "play.filters.csp.nonce.enabled" -> false,
        "features.scp08-enabled" -> true,
        "features.release-c-enabled" -> true
      )
    )
  )

  private val controller = instanceOf[AccountController]

  "AccountController" when {

    "handling request to get Account page" must {

      def performAction() = controller.getAccountPage(FakeRequest())

      behave like authBehaviour(() => performAction())

      "display the lead account home page and check for undertaking balance" when {
        "admin loads home page" in {
          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveAllSubsidies(undertakingRef)(undertakingSubsidies.toFuture)
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(fixedDate)
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

              doc.getElementById("undertaking-balance-section-heading").text shouldBe "Undertaking balance"
              doc
                .getElementById("undertaking-balance-section-content")
                .text shouldBe "Your undertaking currently has a remaining balance of €123.45, from your sector allowance of €12.34."

            }
          )
        }

      }

      "display the lead account home page with warning when undertaking is auto suspended" when {
        "admin page loads home - already submitted" in {
          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(
              undertaking.copy(undertakingStatus = Some(UndertakingStatus.suspendedAutomated)).some.toFuture
            )
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveAllSubsidies(undertakingRef)(undertakingSubsidies.toFuture)
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(fixedDate)
            mockGetOrCreate(eori1)(Right(nilJourneyCreate))
          }

          val result = performAction()

          val doc = Jsoup.parse(contentAsString(result))

          verifyAutoSuspendContentForLead(doc = doc, neverSubmitted = true)

        }
        "admin page loads home - never submitted" in {
          val nilJourneyCreate = NilReturnJourney(NilReturnFormPage(None))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockRetrieveUndertaking(eori1)(
              undertaking.copy(undertakingStatus = Some(UndertakingStatus.suspendedAutomated)).some.toFuture
            )
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockRetrieveAllSubsidies(undertakingRef)(emptyUndertakingSubsidies.toFuture)
            mockGetUndertakingBalance(eori1)(Future.successful(Some(undertakingBalance)))
            mockTimeProviderToday(fixedDate)
            mockGetOrCreate(eori1)(Right(nilJourneyCreate))
          }

          val result = performAction()

          val doc = Jsoup.parse(contentAsString(result))

          verifyAutoSuspendContentForLead(doc = doc, neverSubmitted = false)
        }
        "member page loads home" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(
              undertaking1.copy(undertakingStatus = Some(UndertakingStatus.suspendedAutomated)).some.toFuture
            )
            mockGetOrCreate[EligibilityJourney](eori4)(Right(eligibilityJourneyComplete))
            mockGetOrCreate[UndertakingJourney](eori4)(Right(UndertakingJourney()))
            mockRetrieveAllSubsidies(undertakingRef)(emptyUndertakingSubsidies.toFuture)
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

    }
  }

  private def verifyAutoSuspendContentForLead(doc: Document, neverSubmitted: Boolean) = {
    doc.title() shouldBe "Manage your undertaking - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
    if (neverSubmitted) {
      doc
        .getElementById("warning-text")
        .text shouldBe "! Warning Your deadline to submit a report passed on 20 April 2021."
      doc
        .getElementById("suspended-details-li1")
        .text shouldBe "this date was 90 days after you last submitted a report"
      doc
        .getElementById("suspended-details-li2")
        .text shouldBe "you must use this service to report all non-customs subsidy payments you have received between 20 January 2021 and 20 April 2021 to continue claiming de minimis state aid"
    } else {
      doc
        .getElementById("warning-text")
        .text shouldBe "! Warning Your deadline to submit a report passed on 18 April 2021."
      doc
        .getElementById("suspended-details-li1")
        .text shouldBe "this date was 90 days after you registered your undertaking on 18 January 2021"
      doc
        .getElementById("suspended-details-li2")
        .text shouldBe "you must use this service to report all non-customs subsidy payments you have received between 6 April 2018 and 18 April 2021 to continue claiming de minimis state aid"
    }
    doc
      .getElementById("suspended-details-li3")
      .text shouldBe "these payments can include grants and loans, provided as de minimis state aid from government and public authorities, which you must report at least once every 90 days"
    doc
      .getElementById("suspended-details-li4")
      .text shouldBe "if you have received none, you must submit a report that you have received no payments for that period"
  }
}
