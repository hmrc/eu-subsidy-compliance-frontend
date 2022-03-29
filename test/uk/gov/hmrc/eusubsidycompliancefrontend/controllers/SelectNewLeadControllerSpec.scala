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
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.inject.bind
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIEmailParameter, SingleEORIEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendResult, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.FormPages.AddEoriFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NewLeadJourney.Forms.SelectNewLeadFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class SelectNewLeadControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with UndertakingOpsSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[RetrieveEmailService].toInstance(mockRetrieveEmailService),
    bind[SendEmailService].toInstance(mockSendEmailService),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |    promote-other-as-lead-to-be-template-en = "template_BE_as_lead_EN"
                                   |    promote-other-as-lead-to-be-template-cy = "template_BE_as_lead_CY"
                                   |    promote-other-as-lead-to-lead-template-en = "template_BE_as_lead_mail_to_lead_EN"
                                   |    promote-other-as-lead-to-lead-template-cy = "template_BE_as_lead_mail_to_lead_CY"
                                   |  }
                                   |""".stripMargin)
    )
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
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to put New lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Right(None))
            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        "new lead journey is blank " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Right(None))
            mockPut[NewLeadJourney](NewLeadJourney(), eori1)(Right(NewLeadJourney()))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title", undertaking.name),
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
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("selectNewLead.title", undertaking1.name),
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

      def performAction(data: (String, String)*)(lang: String) = controller
        .postSelectNewLead(
          FakeRequest()
            .withCookies(Cookie("PLAY_LANG", lang))
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no!")
        val emailParamsBE = SingleEORIEmailParameter(eori4, undertaking1.name, undertakingRef, "promoteAsLeadEmailToBE")

        def update(j: NewLeadJourney) = j.copy(selectNewLead = SelectNewLeadFormPage(eori4.some))

        "call to update new lead journey fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)(English.code)))
        }

        "call to fetch business entity email address fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockRetrieveEmail(eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)(English.code)))
        }

        "call to fetch lead email address fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParamsBE, "template_BE_as_lead_EN")(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)(English.code)))
        }

        "language is other than english /welsh" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
          }
          assertThrows[Exception](await(performAction("selectNewLead" -> eori4)("fr")))
        }

      }

      "show the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
          }
          checkFormErrorIsDisplayed(
            performAction()(English.code),
            messageFromMessageKey("selectNewLead.title", undertaking.name),
            messageFromMessageKey("selectNewLead.error.required")
          )
        }
      }

      "redirect to next page" when {

        def update(j: NewLeadJourney) = j.copy(selectNewLead = SelectNewLeadFormPage(eori4.some))

        def testRedirection(templateIdBE: String, templateIdLead: String, lang: String): Unit = {

          val emailParamsBE =
            SingleEORIEmailParameter(eori4, undertaking1.name, undertakingRef, "promoteAsLeadEmailToBE")
          val emailParamLead =
            DoubleEORIEmailParameter(eori1, eori4, undertaking.name, undertakingRef, "promoteAsLeadEmailToLead")
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[NewLeadJourney](_ => update(NewLeadJourney()), eori1)(
              Right(NewLeadJourney(SelectNewLeadFormPage(eori4.some)))
            )
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParamsBE, templateIdBE)(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParamLead, templateIdLead)(Right(EmailSendResult.EmailSent))
            mockSendAuditEvent(AuditEvent.BusinessEntityPromoted(undertakingRef, "1123", eori1, eori4))
          }
          checkIsRedirect(
            performAction("selectNewLead" -> eori4)(lang),
            routes.SelectNewLeadController.getLeadEORIChanged()
          )
        }

        "the language selected by user is  English" in {
          testRedirection("template_BE_as_lead_EN", "template_BE_as_lead_mail_to_lead_EN", English.code)
        }

        "the language selected by user is  Welsh" in {
          testRedirection("template_BE_as_lead_CY", "template_BE_as_lead_mail_to_lead_CY", Welsh.code)
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(English.code))
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
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to fetch new lead journey came back with None" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to fetch new lead journey came back with response but there is no selected EORI in it" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Right(NewLeadJourney().some))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to update business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
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
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
            mockUpdate[BusinessEntityJourney](
              _ =>
                update(
                  businessEntityJourneyLead
                    .copy(eori = AddEoriFormPage(eori4.some))
                ),
              eori1
            )(Right(businessEntityJourneyLead))

            mockPut[NewLeadJourney](NewLeadJourney(), eori)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[Undertaking](eori1)(Right(undertaking.some))
          mockGet[NewLeadJourney](eori1)(Right(newLeadJourney.some))
          mockUpdate[BusinessEntityJourney](
            _ =>
              update(
                businessEntityJourneyLead
                  .copy(eori = AddEoriFormPage(eori4.some))
              ),
            eori1
          )(Right(businessEntityJourneyLead))
          mockPut[NewLeadJourney](NewLeadJourney(), eori)(Right(NewLeadJourney()))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("leadEORIChanged.title"),
          { doc =>
            val htmlText = doc.select(".govuk-body").html()
            htmlText should include regex messageFromMessageKey("leadEORIChanged.p2", eori4, undertaking1.name)

            htmlText should include regex messageFromMessageKey(
              "leadEORIChanged.link",
              routes.AccountController.getAccountPage().url
            )

            val htmlText1 = doc.select(".govuk-panel").html()
            htmlText1 should include regex messageFromMessageKey("leadEORIChanged.subtitle", eori4, undertaking1.name)

          },
          isLeadJourney = true
        )

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

  }

}
