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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.catsSyntaxOptionId
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{RetrieveEmailConnector, SendEmailConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendRequest, EmailSendResult, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class EmailServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures with DefaultAwaitTimeout {

  private val templatedId: String = "templateId1"

  private val fakeConfig = new AppConfig(
    Configuration.from(Map(
      "email-send" -> Map[String, String](
        "create-undertaking-template-en" -> templatedId,
        "send.add-member-to-be-template-en" -> templatedId,
        "add-member-to-be-template-en" -> templatedId,
        "add-member-to-lead-template-en" -> templatedId,
        "remove-member-to-be-template-en" -> templatedId,
        "remove-member-to-lead-template-en" -> templatedId,
        "promote-other-as-lead-to-be-template-en" -> templatedId,
        "promote-other-as-lead-to-lead-template-en" -> templatedId,
        "member-remove-themself-email-to-be-template-en" -> templatedId,
        "member-remove-themself-email-to-lead-template-en" -> templatedId,
        "promoted-themself-email-to-new-lead-template-en" -> templatedId,
        "removed_as_lead-email-to-old-lead-template-en" -> templatedId,
      )
    )),
    new ContactFrontendConfig(Configuration.empty)
  )

  private val emptyHeaders = Map.empty[String, Seq[String]]

  private val validEmailResponseJson = Json.toJson(validEmailResponse)
  private val inValidEmailResponseJson = Json.toJson(inValidEmailResponse)
  private val undeliverableResponseJson = Json.toJson(undeliverableEmailResponse)

  private val mockSendEmailConnector: SendEmailConnector = mock[SendEmailConnector]
  private val mockRetrieveEmailConnector = mock[RetrieveEmailConnector]

  private def mockSendEmail(emailSendRequest: EmailSendRequest)(result: Either[ConnectorError, HttpResponse]) =
    (mockSendEmailConnector
      .sendEmail(_: EmailSendRequest)(_: HeaderCarrier))
      .expects(emailSendRequest, *)
      .returning(result.toFuture)

  private def mockRetrieveEmail(eori: EORI)(result: Either[ConnectorError, HttpResponse]) =
    (mockRetrieveEmailConnector
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result.toFuture)

  private val service = new EmailService(
    fakeConfig,
    mockSendEmailConnector,
    mockRetrieveEmailConnector,
    Configuration.empty
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val fakeRequest = AuthenticatedEscRequest("Foo", "Bar", FakeRequest(), EORI("GB121212121212"))
  private implicit val mockMessagesApi: MessagesApi = mock[MessagesApi]
  private val mockMessages = mock[Messages]

  // TODO - make send email method private and test indirectly
  "SendEmailHelperService" when {

    "retrieveEmailAddressAndSendEmail is called" must {

      "return an error" when {

        "the email retrieval fails" in {
          mockRetrieveEmail(eori1)(Left(ConnectorError(new RuntimeException())))
          val result = service.retrieveEmailAddressAndSendEmail(eori1, None, "createUndertaking", undertaking, undertakingRef, None)
          result.failed.futureValue shouldBe a[ConnectorError]
        }

        "no email address is found" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, inValidEmailResponseJson, emptyHeaders)))
          // TODO - factor this out if it works :/
          (mockMessagesApi.preferred(_: RequestHeader)).expects(*).returning(mockMessages)
          (() => mockMessages.lang).expects().returning(Lang("en"))
          val result = service.retrieveEmailAddressAndSendEmail(eori1, None, "createUndertaking", undertaking, undertakingRef, None)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "the email address is undeliverable" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, undeliverableResponseJson, emptyHeaders)))
          // TODO - factor this out if it works :/
          (mockMessagesApi.preferred(_: RequestHeader)).expects(*).returning(mockMessages)
          (() => mockMessages.lang).expects().returning(Lang("en"))
          val result = service.retrieveEmailAddressAndSendEmail(eori1, None, "createUndertaking", undertaking, undertakingRef, None)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "there is an error sending the email" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          // TODO - factor this out if it works :/
          (mockMessagesApi.preferred(_: RequestHeader)).expects(*).returning(mockMessages)
          (() => mockMessages.lang).expects().returning(Lang("en"))
          mockSendEmail(emailSendRequest)(Left(ConnectorError("Error")))
          val result = service.retrieveEmailAddressAndSendEmail(eori1, None, "createUndertaking", undertaking, undertakingRef, None)
          result.failed.futureValue shouldBe a[ConnectorError]
        }

      }

      "return success" when {

        "the email is sent successfully" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          // TODO - factor this out if it works :/
          (mockMessagesApi.preferred(_: RequestHeader)).expects(*).returning(mockMessages)
          (() => mockMessages.lang).expects().returning(Lang("en"))
          mockSendEmail(emailSendRequest)(Right(HttpResponse(ACCEPTED, "")))
          val result = service.retrieveEmailAddressAndSendEmail(eori1, None, "createUndertaking", undertaking, undertakingRef, None)
          result.futureValue shouldBe EmailSent
        }

      }

    }

    "handling request to send email" must {

      "return an error" when {

        "the http call fails" in {
          mockSendEmail(emailSendRequest)(Left(ConnectorError("")))
          val result = service.sendEmail(validEmailAddress, emailParameter, templatedId)
          assertThrows[RuntimeException](await(result))
        }
      }

      "return Email sent successfully" when {

        "request came back with status Accepted and request can be parsed" in {
          mockSendEmail(emailSendRequest)(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(validEmailAddress, emailParameter, templatedId)
          await(result) shouldBe EmailSendResult.EmailSent
        }
      }

      "return Email sent failure" when {

        "request came back with status != Accepted " in {
          mockSendEmail(emailSendRequest)(Right(HttpResponse(OK, "")))
          val result = service.sendEmail(validEmailAddress, emailParameter, templatedId)
          await(result) shouldBe EmailSendResult.EmailSentFailure
        }

      }

    }

  }


  "handling request to retrieve email by eori" must {

    "return an error" when {

      "the http call fails" in {
        mockRetrieveEmail(eori1)(Left(ConnectorError("")))
        val result = service.retrieveEmailByEORI(eori1)
        assertThrows[RuntimeException](await(result))
      }

      "the http response doesn't come back with status 200(OK) or 404" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(BAD_REQUEST, validEmailResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        assertThrows[RuntimeException](await(result))
      }

      "there is no json in the response" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, "hi")))
        val result = service.retrieveEmailByEORI(eori1)
        assertThrows[RuntimeException](await(result))
      }

      "the json in the response can't be parsed" in {
        val json = Json.parse("""{ "a" : 1 }""")

        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, json, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        assertThrows[RuntimeException](await(result))
      }

    }

    "return successfully" when {

      "the http call return with 200 and valid email address response" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(VerifiedEmail, validEmailAddress.some)
      }

      "the http call return with 404 " in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(NOT_FOUND, " ")))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)
      }

      "the http call return with 200 but the email is Undeliverable " in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, undeliverableResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnDeliverableEmail, undeliverableEmailAddress.some)
      }

      "the http call return with 200 but the email is invalid " in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, inValidEmailResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnVerifiedEmail, inValidEmailAddress.some)
      }
    }
  }

}
