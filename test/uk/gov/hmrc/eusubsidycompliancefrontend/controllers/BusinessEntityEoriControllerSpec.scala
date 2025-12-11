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
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{businessEntityJourney, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

class BusinessEntityEoriControllerSpec
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

  private val controller = instanceOf[BusinessEntityEoriController]

  private val invalidPrefixEoris = List("GA123456789012", "AB123456789012", "12345678901212")
  private val invalidLengthEoris = List("GB1234567890", "GB12345678901234")

  "BusinessEntityEoriControllerSpec" when {

    "handling request to get EORI Page" must {
      def performAction = controller.getEori(FakeRequest(GET, routes.BusinessEntityEoriController.getEori.url))

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))

        }

      }

      "display the page" when {

        def test(businessEntityJourney: BusinessEntityJourney): Unit = {
          val previousUrl = routes.AddBusinessEntityController.getAddBusinessEntity().url
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }

          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("businessEntityEori.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl

              val input = doc.select(".govuk-input").attr("value")
              input shouldBe businessEntityJourney.eori.value.getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityEoriController.postEori.url

              doc.getElementById("businessEntityEoriTitleId").text() shouldBe "Business EORI Number"
              doc
                .getElementById("businessEntityEoriP1Id")
                .text() shouldBe "We need to know the EORI number of the business you want to add."
              doc
                .getElementById("businessEntityEoriP2Id")
                .text() shouldBe "The first 2 letters will be the country code, GB. This is followed by 12 or 15 digits, like GB123456123456."
              doc
                .getElementById("businessEntityEoriP3Id")
                .text() shouldBe "This is the same as, and linked with any XI EORI number you may also have. That means that if you have GB123456123456, the XI version of it would be XI123456123456."
            }
          )
        }

        "user hasn't already answered the question" in {
          test(
            BusinessEntityJourney().copy(
              addBusiness = AddBusinessFormPage(true.some)
            )
          )
        }

        "user has already answered the question with prefix GB" in {
          test(
            BusinessEntityJourney().copy(
              addBusiness = AddBusinessFormPage(true.some),
              eori = AddEoriFormPage(eori1.some)
            )
          )
        }

      }

      "redirect " when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }

        "call to get business entity journey came back empty" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction, routes.AddBusinessEntityController.getAddBusinessEntity().url)

        }
      }

    }

    "handling request to Post EORI page" must {

      def performAction(data: (String, String)*) = controller
        .postEori(
          FakeRequest(POST, routes.BusinessEntityEoriController.getEori.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get previous uri fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "show a form error" when {

        def test(data: (String, String)*)(errorMessageKey: String): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }
          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("businessEntityEori.title"),
            messageFromMessageKey(errorMessageKey)
          )
        }

        def testEORIvalidation(
          data: (String, String)*
        )(retrieveResponse: Either[ConnectorError, Option[Undertaking]], errorMessageKey: String): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRetrieveUndertakingWithErrorResponse(eori4)(retrieveResponse)
          }
          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("businessEntityEori.title"),
            messageFromMessageKey(errorMessageKey)
          )
        }

        "No eori is submitted" in {
          test("businessEntityEori" -> "")("error.businessEntityEori.required")
        }

        "invalid eori is submitted" in {
          invalidPrefixEoris.foreach { eori =>
            withClue(s" For eori :: $eori") {
              test("businessEntityEori" -> eori)("businessEntityEori.regex.error")
            }

          }
        }

        "eori is submitted has invalid length" in {
          invalidLengthEoris.foreach { eori =>
            withClue(s" For eori :: $eori") {
              test("businessEntityEori" -> eori)("businessEntityEori.error.incorrect-length")
            }

          }
        }

        "eori submitted is already in use" in {
          testEORIvalidation("businessEntityEori" -> "GB123456789010")(
            Right(undertaking1.some),
            "businessEntityEori.eoriInUse"
          )
        }

        "eori submitted is not stored in SMTP" in {
          testEORIvalidation("businessEntityEori" -> "GB123456789010")(
            Left(ConnectorError(UpstreamErrorResponse("EORI not present in SMTP", 406))),
            "error.businessEntityEori.required"
          )
        }

      }

      "redirect to the account home page" when {

        "user is not an add business entity page" in {
          testLeadOnlyRedirect(() => performAction())
        }

        "user is an undertaking lead and eori entered prefixed with GB (and spaces)" in {
          val businessEntityJourney = BusinessEntityJourney()
            .copy(
              addBusiness = AddBusinessFormPage(true.some),
              eori = AddEoriFormPage(None)
            )

          List("GB123456789010", "GB123456789013", "GB 123 456 789 999").foreach { eoriEntered =>
            withClue(s" For eori entered :: $eoriEntered") {
              val enteredEoriWithNoSpaces = eoriEntered.replaceAll(" ", "")
              val validEori = EORI(enteredEoriWithNoSpaces)
              inSequence {
                mockAuthWithEnrolmentAndValidEmail()
                mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
                mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))

                mockRetrieveUndertakingWithErrorResponse(validEori)(Right(None))
                mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
                mockAddMember(undertakingRef, BusinessEntity(validEori, leadEORI = false))(Right(undertakingRef))
                mockSendEmail(validEori, AddMemberToBusinessEntity, undertaking)(Right(EmailSent))
                mockSendEmail(eori1, validEori, AddMemberToLead, undertaking)(Right(EmailSent))
                mockSendAuditEvent(AuditEvent.BusinessEntityAdded(undertakingRef, "1123", eori1, validEori))
              }
              checkIsRedirect(
                performAction("businessEntityEori" -> eoriEntered),
                routes.AddBusinessEntityController
                  .startJourney(businessAdded = Some(true), newlyAddedEoriOpt = Some(validEori))
                  .url
              )
            }
          }
        }
        "user is updating an existing business entity (amend journey)" in {
          val oldEori = EORI("GB123456789999")
          val newEori = EORI("GB123456789010")

          val amendBusinessEntityJourney = BusinessEntityJourney(
            addBusiness = AddBusinessFormPage(true.some),
            eori = AddEoriFormPage(None),
            oldEORI = oldEori.some // This makes isAmend return true
          )

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(amendBusinessEntityJourney.some))

            mockRetrieveUndertakingWithErrorResponse(newEori)(Right(None))
            mockGet[BusinessEntityJourney](eori1)(Right(amendBusinessEntityJourney.some))
            mockRemoveMember(undertakingRef, BusinessEntity(oldEori, leadEORI = false))(Right(undertakingRef))
            mockSendEmail(newEori, AddMemberToBusinessEntity, undertaking)(Right(EmailSent))
            mockSendEmail(eori1, newEori, AddMemberToLead, undertaking)(Right(EmailSent))
            mockSendAuditEvent(AuditEvent.BusinessEntityUpdated(undertakingRef, "1123", eori1, newEori))
          }

          checkIsRedirect(
            performAction("businessEntityEori" -> "GB123456789010"),
            routes.AddBusinessEntityController
              .startJourney(businessAdded = Some(true), newlyAddedEoriOpt = Some(newEori))
              .url
          )
        }
      }
    }

  }

}
