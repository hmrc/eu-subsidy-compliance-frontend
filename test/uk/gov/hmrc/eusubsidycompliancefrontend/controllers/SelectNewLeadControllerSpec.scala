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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{BusinessEntityJourney, NewLeadJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.{EmailNotSent, EmailSent}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedOtherAsLeadToBusinessEntity, PromotedOtherAsLeadToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.AddEoriFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NewLeadJourney.Forms.SelectNewLeadFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class SelectNewLeadControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[AuditService].toInstance(mockAuditService)
  )

  private val controller = instanceOf[SelectNewLeadController]

  "SelectNewLeadControllerSpec" when {

    "handling request to get Select New Lead" must {

      def performAction() = controller.getSelectNewLead(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[NewLeadJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" when {

        "new lead journey is blank " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[NewLeadJourney](eori1)(Right(NewLeadJourney()))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage().url
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty shouldBe true

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SelectNewLeadController.postSelectNewLead().url
            }
          )

        }
        "new lead journey already exists" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockGetOrCreate[NewLeadJourney](eori1)(Right(newLeadJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage().url
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.attr("value") shouldBe eori4

              val radioOptions = doc.select(".govuk-radios__item")
              radioOptions.size() shouldBe undertaking1.undertakingBusinessEntity.filterNot(_.leadEORI).size

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SelectNewLeadController.postSelectNewLead().url
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

    "handling request to post select new lead" must {

      def performAction(data: (String, String)*) = controller
        .postSelectNewLead(
          FakeRequest(POST, "/")
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no!")

        def update(j: NewLeadJourney) = j.copy(selectNewLead = SelectNewLeadFormPage(eori4.some))

        "call to update new lead journey fails" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)))
        }

        "call to send email fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockSendAuditEvent(AuditEvent.BusinessEntityPromoted(undertakingRef, "1123", eori1, eori4))
            mockSendEmail(eori1, eori4, PromotedOtherAsLeadToLead, undertaking)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)))
        }

      }

      "show the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title"),
            messageFromMessageKey("selectNewLead.error.required")
          )
        }
      }

      "redirect to next page" when {

        def update(j: NewLeadJourney) = j.copy(selectNewLead = SelectNewLeadFormPage(eori4.some))

        def testRedirection(nextCall: String): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockSendAuditEvent(AuditEvent.BusinessEntityPromoted(undertakingRef, "1123", eori1, eori4))
            mockSendEmail(eori1, eori4, PromotedOtherAsLeadToLead, undertaking)(Right(EmailSendResult.EmailSent))
            mockSendEmail(eori4, PromotedOtherAsLeadToBusinessEntity, undertaking)(Right(EmailSendResult.EmailSent))
          }
          checkIsRedirect(
            performAction("selectNewLead" -> eori4),
            nextCall
          )
        }

        "for a valid request" in {
          testRedirection(routes.SelectNewLeadController.getLeadEORIChanged().url)
        }

        "email address of BE is unverified" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockSendAuditEvent(AuditEvent.BusinessEntityPromoted(undertakingRef, "1123", eori1, eori4))
            mockSendEmail(eori1, eori4, PromotedOtherAsLeadToLead, undertaking)(Right(EmailSent))
            mockSendEmail(eori4, PromotedOtherAsLeadToBusinessEntity, undertaking)(Right(EmailNotSent))
          }
          checkIsRedirect(
            performAction("selectNewLead" -> eori4),
            routes.SelectNewLeadController.emailNotVerified().url
          )

        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get lead EORI changed" must {

      def performAction() = controller.getLeadEORIChanged(FakeRequest())
      behave like authBehaviour(() => performAction())

      def update(b: BusinessEntityJourney) = b.copy(isLeadSelectJourney = None)

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[NewLeadJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to fetch new lead journey came back with response but there is no selected EORI in it" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[NewLeadJourney](eori1)(Right(NewLeadJourney().some))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to update business entity journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
            mockUpdate[BusinessEntityJourney](
              _ => update(businessEntityJourneyLead.copy(eori = AddEoriFormPage(eori4.some))),
              eori1
            )(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to reset business entity journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
            mockUpdate[BusinessEntityJourney](
              _ =>
                update(
                  businessEntityJourneyLead
                    .copy(eori = AddEoriFormPage(eori4.some))
                ),
              eori1
            )(Right(businessEntityJourneyLead))

            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" in {

        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
          mockUpdate[BusinessEntityJourney](
            _ =>
              update(
                businessEntityJourneyLead
                  .copy(eori = AddEoriFormPage(eori4.some))
              ),
            eori1
          )(Right(businessEntityJourneyLead))
          mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Right(NewLeadJourney()))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("leadEORIChanged.title"),
          { doc =>
            val htmlText = doc.select(".govuk-body").html()
            htmlText should include regex messageFromMessageKey("leadEORIChanged.p2", eori4)

            htmlText should include regex messageFromMessageKey(
              "leadEORIChanged.link",
              routes.AccountController.getAccountPage().url
            )

            val htmlText1 = doc.select(".govuk-panel").html()
            htmlText1 should include regex messageFromMessageKey("leadEORIChanged.subtitle", eori4)

          },
          isLeadJourney = true
        )

      }

      "redirect to next page" when {

        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }

        "call to fetch new lead journey came back with None" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[NewLeadJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.SelectNewLeadController.getSelectNewLead().url)

        }
      }

    }

    "handling request to Email not verified" must {

      def performAction() = controller.emailNotVerified(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to get New lead journey  fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[NewLeadJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to reset New lead journey  fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" in {

        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
          mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Right(NewLeadJourney()))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("emailUnverifiedForLeadPromotion.title", eori4),
          isLeadJourney = true
        )

      }

      "redirect to next page" when {

        "call to fetch new lead journey came back with None" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[NewLeadJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.SelectNewLeadController.getSelectNewLead().url)

        }
      }

    }

  }

}
