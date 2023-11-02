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
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.withGbPrefix
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{businessEntityJourney, undertaking, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.concurrent.Future

class RemoveYourselfBusinessEntityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with ScalaFutures
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |     add-member-to-be-template-en = "template_add_be_EN"
                                   |     add-member-to-be-template-cy = "template_add_be_CY"
                                   |     add-member-to-lead-template-en = "template_add_lead_EN"
                                   |     add-member-to-lead-template-cy = "template_add_lead_CY"
                                   |     remove-member-to-be-template-en = "template_remove_be_EN"
                                   |     remove-member-to-be-template-cy = "template_remove_be_CY"
                                   |     remove-member-to-lead-template-en = "template_remove_lead_EN"
                                   |     remove-member-to-lead-template-cy = "template_remove_lead_CY"
                                   |     member-remove-themself-email-to-be-template-en = "template_remove_yourself_be_EN"
                                   |     member-remove-themself-email-to-be-template-cy = "template_remove_yourself_be_CY"
                                   |     member-remove-themself-email-to-lead-template-en = "template_remove_yourself_lead_EN"
                                   |     member-remove-themself-email-to-lead-template-cy = "template_remove_yourself_lead_CY"
                                   |  }
                                   |""".stripMargin)
    )
  )

  abstract class AddBusinessPageSetup(
    method: String = GET,
    theUndertaking: Undertaking = undertaking,
    theBusinessEntityJourney: BusinessEntityJourney = businessEntityJourney
  ) {

    if (method == GET) {
      inSequence {
        mockAuthWithEnrolmentAndValidEmail()
        mockRetrieveUndertaking(eori1)(theUndertaking.some.toFuture)
        mockGetOrCreate[BusinessEntityJourney](eori1)(Right(theBusinessEntityJourney))
      }
    }

    def verifyAddBusinessPageCommonElements(document: Document, errorPresent: Boolean = false) = {
      val titlePrefix = if (errorPresent) "Error: " else ""
      document.title shouldBe s"${titlePrefix}Businesses in your undertaking - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
      document
        .getElementsByAttributeValue("action", routes.AddBusinessEntityController.postAddBusinessEntity.url)
        .size() shouldBe 1 //verify form is on the page
      document.getElementById("continue").text() shouldBe "Save and continue"
    }

  }

  private val controller = instanceOf[RemoveYourselfBusinessEntityController]

  "RemoveYourselfBusinessEntityControllerSpec" when {
    "handling request to get remove yourself Business entity" must {
      def performAction() = controller.getRemoveYourselfBusinessEntity(FakeRequest())

      "throw technical error" when {

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("removeYourselfBusinessEntity.title"),
            { doc =>
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.AccountController.getAccountPage.url
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputDate match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr(
                "action"
              ) shouldBe routes.RemoveYourselfBusinessEntityController.postRemoveYourselfBusinessEntity.url
            }
          )

        }

        "the user hasn't previously answered the question" in {
          test(undertaking1, None)
        }

      }
    }

    "handling request to post remove yourself business entity" must {

      def performAction(data: (String, String)*) = controller
        .postRemoveYourselfBusinessEntity(
          FakeRequest(POST, routes.RemoveYourselfBusinessEntityController.getRemoveYourselfBusinessEntity.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw a technical error" when {
        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("removeYourselfBusinessEntity.title"),
            messageFromMessageKey("removeYourselfBusinessEntity.error.required")
          )

        }

      }

      "redirect to next page" when {

        val effectiveDate: String = Seq(
          fixedDate.plusDays(1).getDayOfMonth.toString,
          fixedDate.plusDays(1).getMonth.name().toLowerCase().capitalize,
          fixedDate.plusDays(1).getYear.toString
        ).mkString(" ")

        "user selected yes" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockDeleteAll(eori4)(Right(()))
            mockTimeProviderToday(fixedDate)
            mockSendEmail(eori4, MemberRemoveSelfToBusinessEntity, undertaking1, effectiveDate)(Right(EmailSent))
            mockSendEmail(eori1, eori4, MemberRemoveSelfToLead, undertaking1, effectiveDate)(Right(EmailSent))
            mockSendAuditEvent(AuditEvent.BusinessEntityRemovedSelf(undertakingRef, "1123", eori1, eori4))
          }
          checkIsRedirect(
            performAction("removeYourselfBusinessEntity" -> "true"),
            routes.SignOutController.signOut().url
          )
        }

        "user selected no" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeYourselfBusinessEntity" -> "false"),
            routes.AccountController.getAccountPage.url
          )
        }
      }

    }

  }
}
