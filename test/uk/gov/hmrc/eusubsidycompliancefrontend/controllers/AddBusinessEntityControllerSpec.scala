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

class AddBusinessEntityControllerSpec
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

  private val controller = instanceOf[AddBusinessEntityController]

  private val invalidPrefixEoris = List("GA123456789012", "AB123456789012", "12345678901212")
  private val invalidLengthEoris = List("GB1234567890", "GB12345678901234")

  "AddBusinessEntityControllerSpec" when {
    "handling request to get add Business Page" must {

      def performAction = controller.getAddBusinessEntity()(FakeRequest())

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

        "user has added a business entity" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1, businessEntity4)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = controller.getAddBusinessEntity(businessAdded = Some(true))(FakeRequest())
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)
          document.getElementById("business-added-banner").text should endWith("Business added")
          document.getElementById("business-added-warning").text should endWith(
            "You have added one or more businesses. Any payments reported for those businesses in a previous undertaking will be moved to this undertaking. These will affect the sector cap from now on."
          )
        }

        "banner to show business eori has been added" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {
          val newlyAddedEori = "GB123456783436"

          val result = controller.getAddBusinessEntity(businessAdded = Some(true), newlyAddedEoriOpt = Some(newlyAddedEori))(FakeRequest())
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val successBannerText = s"${newlyAddedEori} will be added to this undertaking within 24 hours."
          document.getElementById("added-success-content").text shouldBe successBannerText
        }

        "user has removed a business entity" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {

          val result = controller.getAddBusinessEntity(businessRemoved = Some(true))(FakeRequest())
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          verifyAddBusinessPageCommonElements(document)
          document.getElementById("business-removed-banner").text should endWith("Business removed")
          document.getElementById("business-removed-warning").text should endWith(
            "Payments reported up until the removal of any business will continue to affect your sector cap unless and until they move to another undertaking."
          )
        }

        "banner to show business eori has been removed" in new AddBusinessPageSetup(
          theUndertaking = undertaking.copy(undertakingBusinessEntity = List(businessEntity1)),
          theBusinessEntityJourney = businessEntityJourney.copy(addBusiness = AddBusinessFormPage())
        ) {
          val eoriEntered = "GB123456783436"

          val result = controller.getAddBusinessEntity(businessRemoved = Some(true), removedAddedEoriOpt = Some(eoriEntered))(FakeRequest())
          status(result) shouldBe OK

          val document = Jsoup.parse(contentAsString(result))

          val successBannerText = s"${eoriEntered} will be removed from this undertaking within 24 hours."
          document.getElementById("removed-success-content").text shouldBe successBannerText
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

        "redirect to the account home page" when {

          "user is not an undertaking lead" in {
            testLeadOnlyRedirect(() => performAction)
          }
        }
      }

    }

    "handling request to post add Business Page" must {

      def performAction(data: (String, String)*) = controller.postAddBusinessEntity(
        FakeRequest(POST, routes.AddBusinessEntityController.getAddBusinessEntity().url)
          .withFormUrlEncodedBody(data: _*)
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
          val errorMessage = "Select yes if you need to add another business to your undertaking"

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
          checkIsRedirect(performAction("addBusiness" -> "true"), routes.BusinessEntityEoriController.getEori.url)
        }
      }
      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction())
        }
      }
    }

    "startJourney" must {
      def performAction() = controller.startJourney()(
        FakeRequest(GET, "/some-url")
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
          redirectLocation(result) should contain(routes.AddBusinessEntityController.getAddBusinessEntity().url)

        }
      }
    }
  }
}
