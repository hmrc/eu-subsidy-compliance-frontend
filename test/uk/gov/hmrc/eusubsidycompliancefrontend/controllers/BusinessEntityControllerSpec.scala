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
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{businessEntityJourney, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.concurrent.Future

class BusinessEntityControllerSpec
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
        .getElementsByAttributeValue("action", routes.BusinessEntityController.postAddBusinessEntity.url)
        .size() shouldBe 1 //verify form is on the page
      document.getElementById("continue").text() shouldBe "Save and continue"
    }

  }

  private val controller = instanceOf[BusinessEntityController]

  private val invalidPrefixEoris = List("GA123456789012", "AB123456789012", "12345678901212")
  private val invalidLengthEoris = List("GB1234567890", "GB12345678901234")

  "BusinessEntityControllerSpec" when {

    "handling request to get add Business Page" must {

      def performAction = controller.getAddBusinessEntity(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[BusinessEntityJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction))
        }
      }

      "display the page" when {

        //You only need to add businesses that have received customs subsidies (Customs Duty waivers) or non-customs subsidies.
        "addBusiness page should display Business Hint" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          // Assertion to check the the hint
          val hintElement = document.getElementById("addBusiness-hint")
          hintElement.text() shouldBe messageFromMessageKey("addBusiness.hint")
        }

        //replacing want with need in the "Do you want to add another business?"
        "addBusiness has legend tag message" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          // Find the fieldset element that contains the legend
          val fieldsetElement = document.getElementById("addBusiness-hint").parent()

          // Assertion to check the updated legend text
          val legendText: String = fieldsetElement.select("legend.govuk-fieldset__legend--m").text()
          legendText shouldBe messageFromMessageKey("addBusiness.legend")
        }

        "user has not already answered the question - no added business entities added" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)
          document
            .getElementById("no-other-businesses")
            .text() shouldBe "Other than the undertaking administrator, there are currently no other businesses in your undertaking."
          document.getElementById("addBusiness").hasAttr("checked") shouldBe false
          document.getElementById("addBusiness-2").hasAttr("checked") shouldBe false
        }

        "user has not already answered the question - some added business entities added" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1, businessEntity4)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)
          document.getElementById("no-other-businesses") shouldBe null
          document.getElementsByClass("govuk-summary-list__row").size() shouldBe 1
          document
            .getElementById(s"business-entity-${businessEntity4.businessEntityIdentifier}")
            .text shouldBe businessEntity4.businessEntityIdentifier
          document
            .getElementById(s"remove-link-${businessEntity4.businessEntityIdentifier}")
            .attr(
              "href"
            ) shouldBe s"/report-and-manage-your-allowance-for-customs-duty-waiver-claims/lead-undertaking-remove-business-entity/${businessEntity4.businessEntityIdentifier}"
          document.getElementById("addBusiness").hasAttr("checked") shouldBe false
          document.getElementById("addBusiness-2").hasAttr("checked") shouldBe false
        }

        "user has already answered yes" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1, businessEntity4)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage(Some(true)))
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)

          document.getElementById("addBusiness").hasAttr("checked") shouldBe true
          document.getElementById("addBusiness-2").hasAttr("checked") shouldBe false
        }

        "user has already answered no" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1, businessEntity4)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage(Some(false)))
        ) {

          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)

          document.getElementById("addBusiness").hasAttr("checked") shouldBe false
          document.getElementById("addBusiness-2").hasAttr("checked") shouldBe true
        }

      }

      "redirect to the account home page" when {

        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }

    }

    "handling request to post add Business Page" must {

      def performAction(data: (String, String)*) = controller.postAddBusinessEntity(
        FakeRequest(POST, routes.BusinessEntityController.getAddBusinessEntity.url).withFormUrlEncodedBody(data: _*)
      )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update BusinessEntityJourney fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[BusinessEntityJourney](eori1)(
              Left(ConnectorError(exception))
            )
          }

          assertThrows[Exception](await(performAction("addBusiness" -> "true")))
        }

      }

      "show a form error" when {
        "nothing has been submitted" in new AddBusinessPageSetup(method = POST) {
          val errorMessage = "Select yes if you need to add another business"

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          val result: Future[Result] = performAction()
          status(result) shouldBe BAD_REQUEST

          val document = Jsoup.parse(contentAsString(result))
          verifyAddBusinessPageCommonElements(document = document, errorPresent = true)
          document.select(".govuk-error-summary").select("a").text() shouldBe errorMessage
          document.select(".govuk-error-message").text() shouldBe s"Error: $errorMessage"
        }
      }

      "redirect to the next page" when {
        "user selected No" in new AddBusinessPageSetup(method = POST) {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(performAction("addBusiness" -> "false"), routes.AccountController.getAccountPage.url)
        }

        "user selected Yes" in new AddBusinessPageSetup(method = POST) {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[BusinessEntityJourney](eori1)(
              Right(BusinessEntityJourney(addBusiness = AddBusinessFormPage(true.some)))
            )
          }
          checkIsRedirect(performAction("addBusiness" -> "true"), routes.BusinessEntityController.getEori.url)
        }
      }
      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }

    }

    "handling request to get EORI Page" must {
      def performAction = controller.getEori(FakeRequest(GET, routes.BusinessEntityController.getEori.url))

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
          val previousUrl = routes.BusinessEntityController.getAddBusinessEntity.url
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
              button.attr("action") shouldBe routes.BusinessEntityController.postEori.url
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
          checkIsRedirect(performAction, routes.BusinessEntityController.getAddBusinessEntity.url)

        }
      }

    }

    "handling request to Post EORI page" must {

      def performAction(data: (String, String)*) = controller
        .postEori(
          FakeRequest(POST, routes.BusinessEntityController.getEori.url)
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

          def updatedBusinessJourney() =
            businessEntityJourney.copy(eori = businessEntityJourney.eori.copy(value = None))
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
                mockPut[BusinessEntityJourney](
                  updatedBusinessJourney().copy(addBusiness = AddBusinessFormPage(None)),
                  eori1
                )(Right(BusinessEntityJourney()))
              }
              checkIsRedirect(
                performAction("businessEntityEori" -> eoriEntered),
                routes.BusinessEntityController.getAddBusinessEntity.url
              )
            }
          }
        }
      }
    }

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
              button.attr("action") shouldBe routes.BusinessEntityController.postRemoveYourselfBusinessEntity.url
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
          FakeRequest(POST, routes.BusinessEntityController.getRemoveYourselfBusinessEntity.url)
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

    "handling request to get remove Business entity by Lead" must {
      def performAction = controller.getRemoveBusinessEntity(eori4)(FakeRequest())

      "throw technical error" when {

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction))
        }

      }

      "display the page" when {
        def test(undertaking: Undertaking, inputDate: Option[String]): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking.some.toFuture)
          }
          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("removeBusinessEntity.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputDate match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityController.postRemoveBusinessEntity(eori4).url

              doc.select(".govuk-body").text() should include(CommonTestData.eori4)

            }
          )

        }

        "the user hasn't previously answered the question" in {
          test(undertaking1, None)
        }

      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction)
        }
      }

    }

    "handling request to post remove business entity" must {

      def performAction(data: (String, String)*)(eori: EORI) = controller
        .postRemoveBusinessEntity(eori)(
          FakeRequest(POST, routes.BusinessEntityController.postRemoveBusinessEntity(eori4).url)
            .withFormUrlEncodedBody(data: _*)
        )

      val effectiveDate = LocalDate.of(2022, 10, 9)

      "throw a technical error" when {
        val exception = new Exception("oh no!")

        "call to retrieve undertaking returns undertaking having no BE with that eori" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          assertThrows[Exception](await(performAction()(eori4)))
        }

        "call to remove BE fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeProviderToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

        "call to send email fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeProviderToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockSendEmail(eori4, RemoveMemberToBusinessEntity, undertaking1, "10 October 2022")(
              Left(ConnectorError(new RuntimeException()))
            )
          }
          assertThrows[Exception](await(performAction("removeBusiness" -> "true")(eori4)))
        }

      }

      "display the form error" when {

        "nothing is selected" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction()(eori4),
            messageFromMessageKey("removeBusinessEntity.title"),
            "Select yes if you want to remove a business from your undertaking"
          )

        }

      }

      "redirect to next page" when {

        def testRedirection(date: String): Unit = {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
            mockTimeProviderToday(effectiveDate)
            mockRemoveMember(undertakingRef, businessEntity4)(Right(undertakingRef))
            mockSendEmail(eori4, RemoveMemberToBusinessEntity, undertaking1, date)(Right(EmailSent))
            mockSendEmail(eori1, eori4, RemoveMemberToLead, undertaking1, date)(Right(EmailSent))
            mockSendAuditEvent(AuditEvent.BusinessEntityRemoved(undertakingRef, "1123", eori1, eori4))
          }
          checkIsRedirect(
            performAction("removeBusiness" -> "true")(eori4),
            routes.BusinessEntityController.getAddBusinessEntity.url
          )
        }

        "user select yes as input" in {
          testRedirection("10 October 2022")
        }

        "user selects No as input" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockRetrieveUndertaking(eori4)(undertaking1.some.toFuture)
          }
          checkIsRedirect(
            performAction("removeBusiness" -> "false")(eori4),
            routes.BusinessEntityController.getAddBusinessEntity.url
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(eori4))
        }
      }

    }

    "startJourney" must {
      def performAction() = controller.startJourney(
        FakeRequest(GET, routes.BusinessEntityController.startJourney.url)
      )

      "redirect to add business entity page" when {
        "user starts new journey" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockPut[BusinessEntityJourney](BusinessEntityJourney(), eori1)(Right(BusinessEntityJourney()))
          }

          val result = performAction()

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(routes.BusinessEntityController.getAddBusinessEntity.url)

        }
      }
    }
  }

}
