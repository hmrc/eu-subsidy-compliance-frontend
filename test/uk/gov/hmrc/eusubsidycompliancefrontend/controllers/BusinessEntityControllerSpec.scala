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
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BusinessEntityControllerSpec.CheckYourAnswersRowBE
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, Language, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.getValidEori
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData
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
    with ScalaFutures
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService),
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

  private val controller = instanceOf[BusinessEntityController]

  private val invalidEOris = List("GA1234567890", "AB1234567890")
  private val invalidLengthEOris = List("1234567890", "12345678901234", "GB1234567890")

  "BusinessEntityControllerSpec" when {

    "handling request to get add Business Page" must {

      def performAction() = controller.getAddBusinessEntity(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[BusinessEntityJourney](eori1)(Right(businessEntityJourney))
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[BusinessEntityJourney](_ => update(businessEntityJourney), eori1)(
              Left(ConnectorError(exception))
            )
          }

          assertThrows[Exception](await(performAction("addBusiness" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String): Unit = {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(performAction("addBusiness" -> "false"), routes.AccountController.getAccountPage().url)
        }

        "user selected Yes" in {
          def update(j: BusinessEntityJourney) = j.copy(addBusiness = j.addBusiness.copy(value = Some(true)))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[BusinessEntityJourney](_ => update(BusinessEntityJourney()), eori1)(
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
      def performAction() = controller.getEori(FakeRequest("GET", routes.BusinessEntityController.getEori().url))

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" when {

        def test(businessEntityJourney: BusinessEntityJourney): Unit = {
          val previousUrl = routes.BusinessEntityController.getAddBusinessEntity().url
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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

      "redirect " when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(performAction)
        }

        "call to get business entity journey came back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity().url)

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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
        )(retrieveResponse: Either[ConnectorError, Option[Undertaking]], errorMessageKey: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
            Left(ConnectorError(UpstreamErrorResponse("EORI not present in SMTP", 406))),
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
                mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "redirect" when {

        "call to fetch Business Entity journey returns journey without eori" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.copy(eori = AddEoriFormPage()).some))
          }
          redirectLocation(performAction()) shouldBe Some(routes.BusinessEntityController.getEori().url)
        }

        "call to fetch Business Entity journey returns Nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity().url)
        }
      }

      "display the page" in {

        val previousUrl = routes.BusinessEntityController.getEori().url
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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

        "call to get undertaking return undertaking without undertaking ref" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.copy(reference = None).some.toFuture)
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity returns nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to get business entity  return  without EORI" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.copy(eori = AddEoriFormPage(None)).some))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to add member to BE undertaking fails" in {

          val businessEntity = BusinessEntity(eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")(English.code)))
        }

        "call to send email fails" in {
          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockSendEmail(eori2, "addMemberEmailToBE", undertaking)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("cya" -> "true")(Language.English.code)))
        }

        "language is unsupported" in {
          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
          resetBusinessJourney: BusinessEntityJourney
        ): Unit = {
          val businessEntity = BusinessEntity(eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockSendEmail(eori2, "addMemberEmailToBE", undertaking)(Right(EmailSent))
            mockSendEmail(eori1, eori2, "addMemberEmailToLead", undertaking)(Right(EmailSent))
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(businessEntityAddedEvent)
            mockPut[BusinessEntityJourney](resetBusinessJourney, eori1)(Right(BusinessEntityJourney()))
          }
          checkIsRedirect(performAction("cya" -> "true")(English.code), nextCall)
        }

        def testRedirectionLang(lang: Language): Unit = {

          val businessEntity = BusinessEntity(businessEntityIdentifier = eori2, leadEORI = false)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney1.some))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockSendEmail(eori2, "addMemberEmailToBE", undertaking)(Right(EmailSent))
            mockSendEmail(eori1, eori2, "addMemberEmailToLead", undertaking)(Right(EmailSent))
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(businessEntityAddedEvent)
            mockPut[BusinessEntityJourney](BusinessEntityJourney(), eori1)(Right(BusinessEntityJourney()))
          }
          checkIsRedirect(
            performAction("cya" -> "true")(lang.code),
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }

        "all api calls are successful and English language is selected" in {
          testRedirectionLang(English)
        }

        "all api calls are successful and Welsh language is selected" in {
          testRedirectionLang(Welsh)
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

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockRemoveMember(
              undertakingRef,
              businessEntity.copy(businessEntityIdentifier = businessEntityJourney.oldEORI.get)
            )(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity)(Right(undertakingRef))
            mockSendEmail(eori2, "addMemberEmailToBE", undertaking)(Right(EmailSent))
            mockSendEmail(eori1, eori2, "addMemberEmailToLead", undertaking)(Right(EmailSent))
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
            mockAuthWithEORIEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithEORIEnrolment(eori4)
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

      def performAction(data: (String, String)*) = controller
        .postRemoveYourselfBusinessEntity(
          FakeRequest("POST", routes.BusinessEntityController.getRemoveYourselfBusinessEntity().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw a technical error" when {
        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEORIEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEORIEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("removeYourselfBusinessEntity.title", undertaking1.name),
            messageFromMessageKey("removeYourselfBusinessEntity.error.required", undertaking1.name)
          )

        }

      }

      "redirect to next page" when {

        "user selected yes as input" when {
          inSequence {
            mockAuthWithEORIEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeYourselfBusinessEntity" -> "true"),
            routes.SignOutController.signOut().url
          )
        }

        "user selected No as input" in {
          inSequence {
            mockAuthWithEORIEnrolment(eori4)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeYourselfBusinessEntity" -> "false"),
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
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
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

              doc.select(".govuk-hint").text() should include(CommonTestData.eori4)

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
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()(eori4)))
        }

        "call to remove BE fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

        "call to send email fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockSendEmail(eori4, "removeMemberEmailToBE", undertaking1, "10 October 2022")(Left(ConnectorError(new RuntimeException())))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
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

        def testRedirection(lang: Language, date: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockSendEmail(eori4, "removeMemberEmailToBE", undertaking1, date)(Right(EmailSent))
            mockSendEmail(eori1, eori4, "removeMemberEmailToLead", undertaking1, date)(Right(EmailSent))
            mockDelete[Undertaking](eori1)(Right(()))
            mockSendAuditEvent(AuditEvent.BusinessEntityRemoved(undertakingRef, "1123", eori1, eori4))
          }
          checkIsRedirect(
            performAction("removeBusiness" -> "true")(eori4, lang.code),
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }

        "user select yes as input" when {

          "User has selected English language" in {
            testRedirection(English, "10 October 2022")
          }

          "User has selected Welsh language" in {
            testRedirection(Welsh, "10 Hydref 2022")
          }

        }

        "user selects No as input" in {
          inSequence {
            mockAuthWithNecessaryEnrolment(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
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
