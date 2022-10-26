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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.withGbPrefix
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate

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

  private val controller = instanceOf[BusinessEntityController]

  private val invalidPrefixEoris = List("GA1234567890", "AB1234567890")
  private val invalidLengthEoris = List("1234567890", "12345678901234", "GB1234567890")

  "BusinessEntityControllerSpec" when {

    "handling request to get add Business Page" must {

      def performAction() = controller.getAddBusinessEntity(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get business entity journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGetOrCreate[BusinessEntityJourney](eori1)(Right(businessEntityJourney))
          }

          checkPageIsDisplayed(
            performAction(),
            if (undertaking.undertakingBusinessEntity.size > 1)
              messageFromMessageKey("addBusiness.businesses-added.legend")
            else messageFromMessageKey("addBusiness.legend"),
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
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("addBusiness.title"),
            messageFromMessageKey(errorMessage)
          )
        }

        "nothing has been submitted" in {
          displayErrorTest()("addBusiness.error.required")
        }

      }

      "redirect to the next page" when {

        "user selected No" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(performAction("addBusiness" -> "false"), routes.AccountController.getAccountPage().url)
        }

        "user selected Yes" in {
          def update(j: BusinessEntityJourney) = j.copy(addBusiness = j.addBusiness.copy(value = Some(true)))

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
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
            mockAuthWithEnrolmentAndValidEmail()
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

        "user is not an add business entity page" in {
          testLeadOnlyRedirect(() => performAction())
        }

        "user is an undertaking lead and eori entered prefixed with/without GB" in {
          val businessEntityJourney = BusinessEntityJourney()
            .copy(
              addBusiness = AddBusinessFormPage(true.some),
              eori = AddEoriFormPage(None)
            )

          def updatedBusinessJourney() =
              businessEntityJourney.copy(eori = businessEntityJourney.eori.copy(value = None))
          List("123456789010", "GB123456789013").foreach { eoriEntered =>
            withClue(s" For eori entered :: $eoriEntered") {
              val validEori = EORI(withGbPrefix(eoriEntered))
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
                mockPut[BusinessEntityJourney](updatedBusinessJourney().copy(addBusiness = AddBusinessFormPage(None)), eori1)(Right(BusinessEntityJourney()))
              }
              checkIsRedirect(
                performAction("businessEntityEori" -> eoriEntered),
                routes.BusinessEntityController.getAddBusinessEntity().url
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

        val effectiveDate = Seq(
          fixedDate.plusDays(1).getDayOfMonth,
          fixedDate.plusDays(1).getMonth.name().toLowerCase().capitalize,
          fixedDate.plusDays(1).getYear
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
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          }
          assertThrows[Exception](await(performAction()))
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
          testLeadOnlyRedirect(performAction)
        }
      }

    }

    "handling request to post remove business entity" must {

      def performAction(data: (String, String)*)(eori: EORI) = controller
        .postRemoveBusinessEntity(eori)(
          FakeRequest("POST", routes.BusinessEntityController.postRemoveBusinessEntity(eori4).url)
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
            messageFromMessageKey("removeBusinessEntity.error.required")
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
            routes.BusinessEntityController.getAddBusinessEntity().url
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
            routes.BusinessEntityController.getAddBusinessEntity().url
          )
        }
      }

      "redirect to the account home page" when {
        "user is not an undertaking lead" in {
          testLeadOnlyRedirect(() => performAction()(eori4))
        }
      }

    }
  }

}

object BusinessEntityControllerSpec {
  final case class CheckYourAnswersRowBE(question: String, answer: String, changeUrl: String)
}
