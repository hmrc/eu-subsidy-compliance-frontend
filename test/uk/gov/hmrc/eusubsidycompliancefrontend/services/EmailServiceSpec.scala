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
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{RetrieveEmailConnector, SendEmailConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.{EmailNotSent, EmailSent}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.CreateUndertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendRequest, EmailTemplate, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class EmailServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures with DefaultAwaitTimeout {

  private val templates = EmailTemplate.values
    .map(t => t.entryName -> "templateId1")
    .toMap

  private val templateConfig = Configuration.from(Map(
    "email-send" -> templates
  ))

  private val fakeAppConfig = {
    new AppConfig(
      templateConfig,
      new ContactFrontendConfig(Configuration.empty)
    )
  }

  private val emptyHeaders = Map.empty[String, Seq[String]]

  private val validEmailResponseJson = Json.toJson(validEmailResponse)
  private val inValidEmailResponseJson = Json.toJson(inValidEmailResponse)
  private val undeliverableResponseJson = Json.toJson(undeliverableEmailResponse)
  private val unverifiedEmailResponseJson = Json.toJson(unverifiedEmailResponse)

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
    fakeAppConfig,
    mockSendEmailConnector,
    mockRetrieveEmailConnector
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "EmailService" when {

    "sendEmail is called" must {

      "return an error" when {

        "the email retrieval fails" in {
          mockRetrieveEmail(eori1)(Left(ConnectorError(new RuntimeException())))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.failed.futureValue shouldBe a[ConnectorError]
        }

        "no email address is found" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, unverifiedEmailResponseJson, emptyHeaders)))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.futureValue shouldBe EmailNotSent
        }

        "the email address is undeliverable" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, undeliverableResponseJson, emptyHeaders)))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.futureValue shouldBe EmailNotSent
        }

        "the email address response is invalid" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, inValidEmailResponseJson, emptyHeaders)))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "there is an error sending the email" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest)(Left(ConnectorError("Error")))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.failed.futureValue shouldBe a[ConnectorError]
        }

      }

      "return success" when {

        "the email is sent successfully with language code en" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest)(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.futureValue shouldBe EmailSent
        }

        "the email is sent successfully with language code cy" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest)(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking)
          result.futureValue shouldBe EmailSent
        }

        "the email is sent successfully with a removeEffectiveDate value" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest.copy(parameters = singleEoriWithDateEmailParameters))(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(eori1, CreateUndertaking, undertaking, dateTime.toString)
          result.futureValue shouldBe EmailSent
        }

        "the email is sent successfully with a second eori" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest.copy(parameters = doubleEoriEmailParameters))(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(eori1, eori2, CreateUndertaking, undertaking)
          result.futureValue shouldBe EmailSent
        }

        "the email is sent successfully with a second eori and a removeEffectiveDate value" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
          mockSendEmail(emailSendRequest.copy(parameters = doubleEoriWithDateEmailParameters))(Right(HttpResponse(ACCEPTED, "")))
          val result = service.sendEmail(eori1, eori2, CreateUndertaking, undertaking, dateTime.toString)
          result.futureValue shouldBe EmailSent
        }

      }

    }

  }

  "retrieveEmailByEORI is called" must {

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

      "the http call returns a 200 and valid email address response" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(VerifiedEmail, validEmailAddress.some)
      }

      "the http call returns a 404" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(NOT_FOUND, " ")))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)
      }

      "the http call returns a 200 but the email is Undeliverable" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, undeliverableResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnDeliverableEmail, undeliverableEmailAddress.some)
      }

      "the http call returns a 200 but the email is invalid" in {
        mockRetrieveEmail(eori1)(Right(HttpResponse(OK, unverifiedEmailResponseJson, emptyHeaders)))
        val result = service.retrieveEmailByEORI(eori1)
        await(result) shouldBe RetrieveEmailResponse(EmailType.UnVerifiedEmail, validEmailAddress.some)
      }
    }
  }

}
