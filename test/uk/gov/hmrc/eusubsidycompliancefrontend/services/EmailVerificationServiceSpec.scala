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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Lang.defaultLang
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.EmailVerificationRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailVerificationStatusResponse, VerificationStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class EmailVerificationServiceSpec
    extends BaseSpec
    with Matchers
    with MockFactory
    with ScalaFutures
    with DefaultAwaitTimeout {

  private val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  private val mockServicesConfig = mock[ServicesConfig]

  private val service = new EmailVerificationService(
    mockEmailVerificationConnector,
    mockServicesConfig
  )

  val nextPageUrl = "/next-page-url"
  val previousPage = Call(GET, "/previous-page-url")

  val responseBody =
    s"""
       |{
       |  "redirectUri": "$nextPageUrl"
       |}""".stripMargin

  def mockVerifyEmail(status: Int = CREATED) = (mockEmailVerificationConnector
    .verifyEmail(_: EmailVerificationRequest)(_: HeaderCarrier, _: ExecutionContext))
    .expects(*, *, *)
    .returning(Right(HttpResponse(status, responseBody)).toFuture)

  def mockServiceConfig() = (mockServicesConfig
    .baseUrl(_: String))
    .expects(*)
    .returning("")
  def mockServiceConfigGetString() = (mockServicesConfig
    .getString(_: String))
    .expects(*)
    .returning("")

  "EmailVerificationService" when {

    "makeVerificationRequest is called" must {

      implicit val messages: Messages = stubMessagesApi().preferred(Seq(defaultLang))
      implicit val request: AuthenticatedEnrolledRequest[AnyContent] = AuthenticatedEnrolledRequest(
        authorityId = "SomeAuthorityId",
        groupId = "SomeGroupId",
        request = FakeRequest(GET, "/"),
        eoriNumber = eori1
      )

      "redirect to the next page if the email verification succeeded" in {
        inSequence {
          mockServiceConfigGetString()
          mockVerifyEmail()
          mockServiceConfig()
        }

        val result = service.makeVerificationRequestAndRedirect(
          "foo@example.com",
          previousPage.url,
          _ => nextPageUrl
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(nextPageUrl)
      }

      "redirect to the previous page if the email verification failed" in {
        inSequence {
          mockServiceConfigGetString()
          mockVerifyEmail(status = BAD_REQUEST)
        }

        val result = service.makeVerificationRequestAndRedirect(
          "foo@example.com",
          previousPage.url,
          _ => nextPageUrl
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(previousPage.url)
      }

    }

  }
  "getEmailVerificationStatus" must {
    "return a verified and unlocked email" in {
      implicit val request: AuthenticatedEnrolledRequest[AnyContent] = AuthenticatedEnrolledRequest(
        authorityId = "SomeAuthorityId",
        groupId = "SomeGroupId",
        request = FakeRequest(GET, "/"),
        eoriNumber = eori1
      )

      val verificationStatus = VerificationStatus(
        emailAddress = "test@example.com",
        verified = true,
        locked = false
      )

      val response = EmailVerificationStatusResponse(
        emails = List(verificationStatus)
      )

      (mockEmailVerificationConnector
        .getVerificationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("SomeAuthorityId", *, *)
        .returning(Future.successful(response))

      val result = service.getEmailVerificationStatus

      whenReady(result) { res =>
        res shouldBe Some(verificationStatus)
      }
    }

    "return None when no verified and unlocked email exists" in {
      implicit val request: AuthenticatedEnrolledRequest[AnyContent] = AuthenticatedEnrolledRequest(
        authorityId = "SomeAuthorityId",
        groupId = "SomeGroupId",
        request = FakeRequest(GET, "/"),
        eoriNumber = eori1
      )

      val lockedEmail = VerificationStatus(
        emailAddress = "locked@example.com",
        verified = true,
        locked = true
      )

      val response = EmailVerificationStatusResponse(
        emails = List(lockedEmail)
      )

      (mockEmailVerificationConnector
        .getVerificationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("SomeAuthorityId", *, *)
        .returning(Future.successful(response))

      val result = service.getEmailVerificationStatus

      whenReady(result) { res =>
        res shouldBe None
      }
    }
  }
}
