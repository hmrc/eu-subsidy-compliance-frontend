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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BusinessEntityControllerSpec.CheckYourAnswersRowBE
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIAndDateEmailParameter, DoubleEORIEmailParameter, SingleEORIAndDateEmailParameter, SingleEORIEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailParameters, EmailSendResult, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, Language, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.getValidEori
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.collection.JavaConverters._

class BusinessEntityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with JourneySupport
    with EmailSupport
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with TimeProviderSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService),
    bind[RetrieveEmailService].toInstance(mockRetrieveEmailService),
    bind[SendEmailService].toInstance(mockSendEmailService),
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

  private val controller = instanceOf[BusinessEntityController]

  private val invalidEOris = List("GA1234567890", "AB1234567890")
  private val invalidLengthEOris = List("1234567890", "12345678901234", "GB1234567890")
  private val currentDate = LocalDate.of(2022, 10, 9)

  "BusinessEntityControllerSpec" when {

    "handling request to get add Business Page" must {

      def performAction() = controller.getAddBusinessEntity(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to store undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        def test(
          input: Option[String],
          businessEntityJourney: BusinessEntityJourney,
          undertaking: Undertaking
        ): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Right(undertaking))
          }

          checkPageIsDisplayed(
            performAction(),
            if (undertaking.undertakingBusinessEntity.size > 1)
              messageFromMessageKey("addBusiness.businesses-added.title", undertaking.name)
            else messageFromMessageKey("addBusiness.empty.title", undertaking.name),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              input match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postAddBusinessEntity().url
            }
          )
        }

        "user hasn't already answered the question" in {
          test(None, BusinessEntityJourney(), undertaking)
        }

        "user hasn't already answered the question and has no BE in the undertaking" in {
          test(None, BusinessEntityJourney(), undertaking.copy(undertakingBusinessEntity = List(businessEntity1)))
        }

        "user has already answered the question" in {
          test(Some("true"), BusinessEntityJourney(addBusiness = AddBusinessFormPage(true.some)), undertaking)
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post add Business Page" must {

      def performAction(data: (String, String)*) = controller.postAddBusinessEntity(
        FakeRequest("POST", routes.BusinessEntityController.getAddBusinessEntity().url).withFormUrlEncodedBody(data: _*)
      )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update BusinessEntityJourney fails" in {

          def update(j: BusinessEntityJourney) = j.copy(addBusiness = j.addBusiness.copy(value = Some(true)))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney), eori)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("addBusiness" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String): Unit = {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("addBusiness.businesses-added.title", undertaking.name),
            messageFromMessageKey(errorMessage, undertaking.name)
          )
        }

        "nothing has been submitted" in {
          displayErrorTest()("addBusiness.error.required")
        }

      }

      "redirect to the next page" when {

        "user selected No" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
          }
          checkIsRedirect(performAction("addBusiness" -> "false"), routes.AccountController.getAccountPage().url)
        }

        "user selected Yes" in {
          def update(j: BusinessEntityJourney) = j.copy(addBusiness = j.addBusiness.copy(value = Some(true)))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockUpdate[BusinessEntityJourney](_ => update(BusinessEntityJourney()), eori)(
              Right(BusinessEntityJourney(addBusiness = AddBusinessFormPage(true.some)))
            )
          }
          checkIsRedirect(performAction("addBusiness" -> "true"), routes.BusinessEntityController.getEori().url)
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get EORI Page" must {
      def performAction() = controller.getEori(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get previous uri fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right("/add-member"))
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to get business entity journey came back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right("/add-member"))
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" when {

        def test(businessEntityJourney: BusinessEntityJourney): Unit = {
          val previousUrl = "add-member"
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right(previousUrl))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("businessEntityEori.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl

              val input = doc.select(".govuk-input").attr("value")
              input shouldBe businessEntityJourney.eori.value.map(_.drop(2)).getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postEori().url
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

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to Post EORI page" must {

      def performAction(data: (String, String)*) = controller
        .postEori(
          FakeRequest("POST", routes.BusinessEntityController.getEori().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get previous uri fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to update business entity journey fails" in {

          def update(j: BusinessEntityJourney) = j.copy(eori = j.eori.copy(value = Some(EORI("123456789010"))))

          val businessEntityJourney = BusinessEntityJourney()
            .copy(
              addBusiness = AddBusinessFormPage(true.some),
              eori = AddEoriFormPage(eori1.some)
            )

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right("add-member"))
            mockRetrieveUndertakingWithErrorResponse(eori4)(Right(None))
            mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney), eori1)(
              Left(ConnectorError(exception))
            )
          }

          assertThrows[Exception](await(performAction("businessEntityEori" -> "123456789010")))

        }

      }

      "show a form error" when {

        def test(data: (String, String)*)(errorMessageKey: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right("add-member"))
          }
          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("businessEntityEori.title"),
            messageFromMessageKey(errorMessageKey)
          )
        }

        def testEORIvalidation(
          data: (String, String)*
        )(retrieveResponse: Either[UpstreamErrorResponse, Option[Undertaking]], errorMessageKey: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGetPrevious[BusinessEntityJourney](eori1)(Right("add-member"))
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
          invalidEOris.foreach { eori =>
            withClue(s" For eori :: $eori") {
              test("businessEntityEori" -> eori)("businessEntityEori.regex.error")
            }

          }
        }

        "eori is submitted has invalid length" in {
          invalidLengthEOris.foreach { eori =>
            withClue(s" For eori :: $eori") {
              test("businessEntityEori" -> eori)("businessEntityEori.error.incorrect-length")
            }

          }
        }

        "eori submitted is already in use" in {
          testEORIvalidation("businessEntityEori" -> "123456789010")(
            Right(undertaking1.some),
            "businessEntityEori.eoriInUse"
          )
        }

        "eori submitted is not stored in SMTP" in {
          testEORIvalidation("businessEntityEori" -> "123456789010")(
            Left(UpstreamErrorResponse("EORI not present in SMTP", 406)),
            "error.businessEntityEori.required"
          )
        }

      }

      "redirect to the account home page" when {

        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }

        "user is an undertaking lead and eori entered prefixed with/without GB" in {
          val businessEntityJourney = BusinessEntityJourney()
            .copy(
              addBusiness = AddBusinessFormPage(true.some),
              eori = AddEoriFormPage(eori1.some)
            )

          def update(j: BusinessEntityJourney, eoriEntered: EORI) =
            j.copy(eori = j.eori.copy(value = Some(eoriEntered)))
          def updatedBusinessJourney(eoriEntered: EORI) =
            businessEntityJourney.copy(eori = businessEntityJourney.eori.copy(value = Some(eoriEntered)))
          List("123456789010", "GB123456789013").foreach { eoriEntered =>
            withClue(s" For eori entered :: $eoriEntered") {
              val validEori = EORI(getValidEori(eoriEntered))
              inSequence {
                mockAuthWithNecessaryEnrolment()
                mockGet[Undertaking](eori1)(Right(undertaking.some))
                mockGetPrevious[BusinessEntityJourney](eori1)(Right("add-member"))
                mockRetrieveUndertakingWithErrorResponse(validEori)(Right(None))
                mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney, validEori), eori1)(
                  Right(updatedBusinessJourney(validEori))
                )
              }
              checkIsRedirect(
                performAction("businessEntityEori" -> eoriEntered),
                routes.BusinessEntityController.getCheckYourAnswers().url
              )
            }

          }

        }
      }

    }

    "handling request to get check your answers page" must {

      def performAction() =
        controller.getCheckYourAnswers(FakeRequest("GET", routes.BusinessEntityController.getCheckYourAnswers().url))

      "throw technical error" when {

        val exception = new Exception("oh no!")
        "Call to fetch Business Entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to fetch Business Entity journey returns Nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch Business Entity journey returns journey without eori" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.copy(eori = AddEoriFormPage()).some))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" in {

        val previousUrl = routes.BusinessEntityController.getEori().url
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[Undertaking](eori1)(Right(undertaking.some))
          mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("businessEntity.cya.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
            val rows =
              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
                val question = element.select(".govuk-summary-list__key").text()
                val answer = element.select(".govuk-summary-list__value").text()
                val changeUrl = element.select(".govuk-link").attr("href")
                CheckYourAnswersRowBE(question, answer, changeUrl)
              }
            rows shouldBe expectedRows(eori1)
          }
        )
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post check yor answers" must {
      def performAction(data: (String, String)*)(lang: String) = controller
        .postCheckYourAnswers(
          FakeRequest("POST", routes.BusinessEntityController.getCheckYourAnswers().url)
            .withCookies(Cookie("PLAY_LANG", lang))
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no")
        val emailParametersBE = SingleEORIEmailParameter(eori2, undertaking.name, undertakingRef, "addMemberEmailToBE")

        "call to get undertaking return undertaking without undertaking ref" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity returns nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity  return  without EORI" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.copy(eori = AddEoriFormPage(None)).some))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to add member to BE undertaking fails" in {

          val businessEntity = BusinessEntity(eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to retrieve email for BE EORI fails" in {

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("cya" -> "true")(Language.English.code)))
        }

        "call to retrieve email for BE EORI returns No Email" in {

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(Language.English.code)))
        }

        "call to retrieve email for lead EORI fails" in {

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, "template_add_be_EN")(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(Language.English.code)))
        }

        "call to retrieve Lead EORI email address returns None" in {

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, "template_add_be_EN")(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(Language.English.code)))
        }

        "language is other than english /welsh" in {
          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")("fr")))
        }

      }

      "post successful for creation" when {

        def testRedirection(
          businessEntityJourney: BusinessEntityJourney,
          nextCall: String,
          resettedBusinessJourney: BusinessEntityJourney
        ): Unit = {
          val businessEntity = BusinessEntity(eori2, leadEORI = false)
          val emailParametersBE =
            SingleEORIEmailParameter(eori2, undertaking.name, undertakingRef, "addMemberEmailToBE")
          val emailParametersLead =
            DoubleEORIEmailParameter(eori1, eori2, undertaking.name, undertakingRef, "addMemberEmailToLead")
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, "template_add_be_EN")(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersLead, "template_add_lead_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(businessEntityAddedEvent)
            mockPut[BusinessEntityJourney](resettedBusinessJourney, eori1)(Right(BusinessEntityJourney()))
          }
          checkIsRedirect(performAction("cya" -> "true")(English.code), nextCall)
        }

        def testRedirectionLang(lang: String, templateIdBE: String, templateIdLead: String): Unit = {

          val emailParametersBE =
            SingleEORIEmailParameter(eori2, undertaking.name, undertakingRef, "addMemberEmailToBE")
          val emailParametersLead =
            DoubleEORIEmailParameter(eori1, eori2, undertaking.name, undertakingRef, "addMemberEmailToLead")

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, templateIdBE)(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersLead, templateIdLead)(Right(EmailSendResult.EmailSent))
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(businessEntityAddedEvent)
            mockPut[BusinessEntityJourney](BusinessEntityJourney(), eori1)(Right(BusinessEntityJourney()))
          }
          checkIsRedirect(
            performAction("cya" -> "true")(lang),
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }

        "all api calls are successful and English language is selected" in {
          testRedirectionLang(Language.English.code, "template_add_be_EN", "template_add_lead_EN")
        }

        "all api calls are successful and Welsh language is selected" in {
          testRedirectionLang(Language.Welsh.code, "template_add_be_CY", "template_add_lead_CY")
        }

        "all api calls are successful and is Select lead journey " in {
          testRedirection(
            businessEntityJourneyLead,
            routes.SelectNewLeadController.getSelectNewLead().url,
            BusinessEntityJourney(isLeadSelectJourney = true.some)
          )
        }

        "all api calls are successful and is normal add business entity journey " in {
          testRedirection(
            businessEntityJourney1,
            routes.BusinessEntityController.getAddBusinessEntity().url,
            BusinessEntityJourney()
          )
        }
      }

      "edit post successful for edit" when {

        def testRedirection(
          businessEntityJourney: BusinessEntityJourney,
          nextCall: String,
          updatedBusinessJourney: BusinessEntityJourney
        ): Unit = {
          val businessEntity = BusinessEntity(eori2, leadEORI = false)
          val emailParametersBE =
            SingleEORIEmailParameter(eori2, undertaking.name, undertakingRef, "addMemberEmailToBE")
          val emailParametersLead =
            DoubleEORIEmailParameter(eori1, eori2, undertaking.name, undertakingRef, "addMemberEmailToLead")
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRemoveMember(
              undertakingRef,
              businessEntity.copy(businessEntityIdentifier = businessEntityJourney.oldEORI.get)
            )(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockRetrieveEmail(eori2)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, "template_add_be_EN")(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersLead, "template_add_lead_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(businessEntityUpdatedEvent)
            mockPut[BusinessEntityJourney](updatedBusinessJourney, eori1)(Right(BusinessEntityJourney()))
          }
          checkIsRedirect(performAction("cya" -> "true")(English.code), nextCall)
        }

        "all api calls are successful and is normal edit business entity journey " in {
          testRedirection(
            businessEntityJourney1.copy(oldEORI = Some(eori4)),
            routes.BusinessEntityController.getAddBusinessEntity().url,
            BusinessEntityJourney()
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(English.code))
        }
      }

    }

    "handling request to get remove yourself Business entity" must {
      def performAction() = controller.getRemoveYourselfBusinessEntity(FakeRequest())

      "throw technical error" when {

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("removeYourselfBusinessEntity.title", undertaking.name),
            { doc =>
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.AccountController.getAccountPage().url
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputDate match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postRemoveYourselfBusinessEntity().url
            }
          )

        }

        "the user hasn't previously answered the question" in {
          test(undertaking1, None)
        }

      }
    }

    "handling request to post remove yourself business entity" must {

      def performAction(data: (String, String)*)(lang: String) = controller
        .postRemoveYourselfBusinessEntity(
          FakeRequest("POST", routes.BusinessEntityController.getRemoveYourselfBusinessEntity().url)
            .withCookies(Cookie("PLAY_LANG", lang))
            .withFormUrlEncodedBody(data: _*)
        )

      "throw a technical error" when {
        val exception = new Exception("oh no!")
        val emailParamBE = SingleEORIAndDateEmailParameter(
          eori4,
          undertaking.name,
          undertakingRef,
          "10 October 2022",
          "removeThemselfEmailToBE"
        )

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()(English.code)))
        }

        "call to remove BE fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeYourselfBusinessEntity" -> "true")(English.code)))
        }

        "call to retrieve email address of the EORI, to be removed, fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeYourselfBusinessEntity" -> "true")(English.code)))
        }

        "call to retrieve email address of the lead EORI, to be removed, fails" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParamBE, "template_remove_yourself_be_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockRetrieveEmail(eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeYourselfBusinessEntity" -> "true")(English.code)))
        }

        "language is other than english /welsh" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(currentDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
          }
          assertThrows[Exception](await(performAction("removeYourselfBusinessEntity" -> "true")("fr")))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()(English.code),
            messageFromMessageKey("removeYourselfBusinessEntity.title", undertaking1.name),
            messageFromMessageKey("removeYourselfBusinessEntity.error.required", undertaking1.name)
          )

        }

      }

      "redirect to next page" when {

        "user select yes as input" when {

          def testRedirection(
            lang: String,
            templateIdBe: String,
            templateIdLead: String,
            effectiveRemovalDate: String
          ): Unit = {

            val emailParamBE = SingleEORIAndDateEmailParameter(
              eori4,
              undertaking.name,
              undertakingRef,
              effectiveRemovalDate,
              "removeThemselfEmailToBE"
            )
            val emailParamLead = DoubleEORIAndDateEmailParameter(
              eori1,
              eori4,
              undertaking.name,
              undertakingRef,
              effectiveRemovalDate,
              "removeThemselfEmailToLead"
            )
            inSequence {
              mockAuthWithEnrolment(eori4)
              mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
              mockTimeToday(currentDate)
              mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
              mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
              mockSendEmail(validEmailAddress, emailParamBE, templateIdBe)(Right(EmailSendResult.EmailSent))
              mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
              mockSendEmail(validEmailAddress, emailParamLead, templateIdLead)(Right(EmailSendResult.EmailSent))
              mockSendAuditEvent(AuditEvent.BusinessEntityRemovedSelf(undertakingRef, "1123", eori1, eori4))
            }
            checkIsRedirect(
              performAction("removeYourselfBusinessEntity" -> "true")(lang),
              routes.SignOutController.signOut().url
            )

          }
          "the language of the application is English" in {
            testRedirection(
              English.code,
              "template_remove_yourself_be_EN",
              "template_remove_yourself_lead_EN",
              "10 October 2022"
            )
          }

          "the language of the application is Welsh" in {
            testRedirection(
              Welsh.code,
              "template_remove_yourself_be_CY",
              "template_remove_yourself_lead_CY",
              "10 Hydref 2022"
            )
          }
        }

        "user selects No as input" in {
          inSequence {
            mockAuthWithEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeYourselfBusinessEntity" -> "false")(English.code),
            routes.AccountController.getAccountPage().url
          )
        }
      }

    }

    "handling request to get remove Business entity by Lead" must {
      def performAction() = controller.getRemoveBusinessEntity(eori4)(FakeRequest())

      "throw technical error" when {

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("removeBusinessEntity.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputDate match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postRemoveBusinessEntity(eori4).url

            }
          )

        }

        "the user hasn't previously answered the question" in {
          test(undertaking1, None)
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post remove business entity" must {

      def performAction(data: (String, String)*)(eori: EORI, language: String = English.code) = controller
        .postRemoveBusinessEntity(eori)(
          FakeRequest("POST", routes.BusinessEntityController.postRemoveBusinessEntity(eori4).url)
            .withCookies(Cookie("PLAY_LANG", language))
            .withFormUrlEncodedBody(data: _*)
        )

      val effectiveDate = LocalDate.of(2022, 10, 9)

      "throw a technical error" when {
        val exception = new Exception("oh no!")

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking.some))
          }
          assertThrows[Exception](await(performAction()(eori4)))
        }

        "call to remove BE fails" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

        "call to fetch business entity email address fails" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

        "call to fetch LeadEORI email address fails" in {
          val emailParameterBE = SingleEORIAndDateEmailParameter(
            eori4,
            undertaking.name,
            undertakingRef,
            "10 October 2022",
            "removeMemberEmailToBE"
          )

          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParameterBE, "template_remove_be_EN")(
              Right(EmailSendResult.EmailSent)
            )
            mockRetrieveEmail(eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()(eori4),
            messageFromMessageKey("removeBusinessEntity.title"),
            messageFromMessageKey("removeBusinessEntity.error.required")
          )

        }

      }

      "redirect to next page" when {

        def testRedirection(
          emailParametersBE: EmailParameters,
          emailParametersLead: EmailParameters,
          templateIdBE: String,
          templateIdLead: String,
          lang: String
        ): Unit = {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockRetrieveEmail(eori4)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersBE, templateIdBE)(Right(EmailSendResult.EmailSent))
            mockRetrieveEmail(eori1)(Right(RetrieveEmailResponse(EmailType.VerifiedEmail, validEmailAddress.some)))
            mockSendEmail(validEmailAddress, emailParametersLead, templateIdLead)(Right(EmailSendResult.EmailSent))
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(AuditEvent.BusinessEntityRemoved(undertakingRef, "1123", eori1, eori4))
          }
          checkIsRedirect(
            performAction("removeBusiness" -> "true")(eori4, lang),
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }

        "user select yes as input" when {

          "User has selected English language" in {

            val emailParameterBE = SingleEORIAndDateEmailParameter(
              eori4,
              undertaking.name,
              undertakingRef,
              "10 October 2022",
              "removeMemberEmailToBE"
            )
            val emailParameterLead = DoubleEORIAndDateEmailParameter(
              eori1,
              eori4,
              undertaking.name,
              undertakingRef,
              "10 October 2022",
              "removeMemberEmailToLead"
            )
            testRedirection(
              emailParameterBE,
              emailParameterLead,
              "template_remove_be_EN",
              "template_remove_lead_EN",
              English.code
            )

          }

          "User has selected Welsh language" in {

            val emailParameterBE = SingleEORIAndDateEmailParameter(
              eori4,
              undertaking.name,
              undertakingRef,
              "10 Hydref 2022",
              "removeMemberEmailToBE"
            )
            val emailParameterLead = DoubleEORIAndDateEmailParameter(
              eori1,
              eori4,
              undertaking.name,
              undertakingRef,
              "10 Hydref 2022",
              "removeMemberEmailToLead"
            )
            testRedirection(
              emailParameterBE,
              emailParameterLead,
              "template_remove_be_CY",
              "template_remove_lead_CY",
              Welsh.code
            )

          }

        }

        "user selects No as input" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeBusiness" -> "false")(eori4),
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(eori4, English.code))
        }
      }

    }
  }

  private def expectedRows(eori: EORI) = List(
    CheckYourAnswersRowBE(
      messageFromMessageKey("businessEntity.cya.eori.label"),
      eori,
      routes.BusinessEntityController.getEori().url
    )
  )

}

object BusinessEntityControllerSpec {
  final case class CheckYourAnswersRowBE(question: String, answer: String, changeUrl: String)
}
