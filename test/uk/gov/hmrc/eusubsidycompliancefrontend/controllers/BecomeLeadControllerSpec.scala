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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.SingleEORIEmailParameter
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.Forms.{BecomeLeadEoriFormPage, TermsAndConditionsFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData._

import scala.concurrent.Future

class BecomeLeadControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with RetrieveEmailSupport
    with SendEmailSupport {

  private val mockEscService = mock[EscService]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[RetrieveEmailService].toInstance(mockRetrieveEmailService),
    bind[SendEmailService].toInstance(mockSendEmailService)
  )

  override def additionalConfig = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |    promoted-themself-email-to-new-lead-template-en = "template_promoted_themself_as_lead_email_to_lead_EN"
                                   |    promoted-themself-email-to-new-lead-template-cy = "template_promoted_themself_as_lead_email_to_lead_CY"
                                   |    removed_as_lead-email-to-old-lead-template-en = "template_removed_as_lead_email_to_previous_lead_EN"
                                   |    removed_as_lead-email-to-old-lead-template-cy = "template_removed_as_lead_email_to_previous_lead_CY"
                                   |  }
                                   |""".stripMargin)
    )
  )

  private val controller = instanceOf[BecomeLeadController]

  def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[Error, UndertakingRef]
  ) =
    (mockEscService
      .addMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)), Future.successful(_)))

  "BecomeLeadControllerSpec" when {

    "handling request to get Become Lead Eori" must {

      def performAction() = controller.getBecomeLeadEori(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")
        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "display the page" when {

        "new lead journey is blank " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Right(None))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockPut[BecomeLeadJourney](newBecomeLeadJourney, eori)(Right(newBecomeLeadJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title", undertaking1.name),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.isEmpty shouldBe true

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BecomeLeadController.postBecomeLeadEori().url
            }
          )

        }
        "new lead journey already exists" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(
              Right(
                newBecomeLeadJourney
                  .copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
                  .some
              )
            )
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("become-admin.title", undertaking1.name),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              selectedOptions.attr("value") shouldBe "true"

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BecomeLeadController.postBecomeLeadEori().url
            }
          )
        }
      }

    }

    "handling request to post Become Lead Eori" must {

      def performAction(data: (String, String)*) = controller
        .postBecomeLeadEori(FakeRequest().withFormUrlEncodedBody(data: _*))

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to update new lead journey fails" in {

          def update(newLeadJourneyOpt: Option[BecomeLeadJourney]) = {
            newLeadJourneyOpt.map(_.copy(becomeLeadEori = BecomeLeadEoriFormPage()))
          }
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[BecomeLeadJourney](_ => update(BecomeLeadJourney().some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("becomeAdmin" -> "true")))
        }

      }

    }

    "handling request to get Become Lead Eori Terms" must {

      def performAction() = controller.getAcceptPromotionTerms(FakeRequest())
      behave like authBehaviour(() => performAction())

      "throw technical error" when {
        val exception = new Exception("oh no!")

        "call to fetch new lead journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[BecomeLeadJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" in {

        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[BecomeLeadJourney](eori1)(
            Right(
              newBecomeLeadJourney
                .copy(becomeLeadEori = newBecomeLeadJourney.becomeLeadEori.copy(value = Some(true)))
                .some
            )
          )
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("become-admin-tandc.title")
        )

      }

    }

    "handling request to get Promotion Confirmation" must {

      def performAction(data: (String, String)*)(lang: String) = controller
        .getPromotionConfirmation(
          FakeRequest()
            .withCookies(Cookie("PLAY_LANG", lang))
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no!")

        "call to fetch become lead journey fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to fetch undertaking fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to fetch undertaking passes but come back wth no undertaking" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to fetch undertaking passes but come back wth undertaking with no undertaking ref" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to fetch undertaking come back with undertaking with logged In EORI absent" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(
              Future.successful(undertaking1.copy(undertakingBusinessEntity = List(businessEntity1)).some)
            )
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to fetch undertaking come back with undertaking with lead EORI absent" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(
              Future.successful(undertaking1.copy(undertakingBusinessEntity = List(businessEntity4)).some)
            )
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to update new lead fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to update old lead fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to retrieve email address of new lead fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to retrieve email address of old lead fails" in {

          val newLeadParams =
            SingleEORIEmailParameter(eori4, undertaking1.name, undertakingRef, "promotedAsLeadToNewLead")
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(validEmailAddress.some))
            mockSendEmail(validEmailAddress, newLeadParams, "template_promoted_themself_as_lead_email_to_lead_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockRetrieveEmail(eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "language is other than english /welsh" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(validEmailAddress.some))
          }
          assertThrows[Exception](await(performAction()("fr")))
        }

      }

      "display the page" when {

        def testDisplay(templateIdNewLead: String, templateIdOldLead: String, lang: String) = {
          val newLeadParams =
            SingleEORIEmailParameter(eori4, undertaking1.name, undertakingRef, "promotedAsLeadToNewLead")
          val oldLeadParams =
            SingleEORIEmailParameter(eori1, undertaking1.name, undertakingRef, "removedAsLeadToOldLead")
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.copy(acceptTerms = TermsAndConditionsFormPage(true.some)).some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(validEmailAddress.some))
            mockSendEmail(validEmailAddress, newLeadParams, templateIdNewLead)(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockSendEmail(validEmailAddress, oldLeadParams, templateIdOldLead)(Right(EmailSendResult.EmailSent))
          }

          checkPageIsDisplayed(
            performAction()(lang),
            messageFromMessageKey("become-admin-confirmation.title")
          )
        }

        "when user has selected English language" in {
          testDisplay(
            "template_promoted_themself_as_lead_email_to_lead_EN",
            "template_removed_as_lead_email_to_previous_lead_EN",
            English.code
          )
        }

        "when user has selected Welsh language" in {
          testDisplay(
            "template_promoted_themself_as_lead_email_to_lead_CY",
            "template_removed_as_lead_email_to_previous_lead_CY",
            Welsh.code
          )

        }

      }

      "redirect to next page" when {

        "become lead journey doesn't contains accept terms" in {
          val newLeadParams =
            SingleEORIEmailParameter(eori4, undertaking1.name, undertakingRef, "promotedAsLeadToNewLead")
          val oldLeadParams =
            SingleEORIEmailParameter(eori1, undertaking1.name, undertakingRef, "removedAsLeadToOldLead")
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockGet[BecomeLeadJourney](eori4)(Right(newBecomeLeadJourney.some))
            mockRetreiveUndertaking(eori4)(Future.successful(undertaking1.some))
            mockAddMember(undertakingRef, businessEntity4.copy(leadEORI = true))(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(leadEORI = false))(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(validEmailAddress.some))
            mockSendEmail(validEmailAddress, newLeadParams, "template_promoted_themself_as_lead_email_to_lead_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockRetrieveEmail(eori1)(Right(validEmailAddress.some))
            mockSendEmail(validEmailAddress, oldLeadParams, "template_removed_as_lead_email_to_previous_lead_EN")(
              Right(EmailSendResult.EmailSent)
            )
          }

          checkIsRedirect(performAction()(English.code), routes.BecomeLeadController.getBecomeLeadEori())

        }
      }

    }

  }

}
