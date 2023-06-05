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

import org.mockito.Mockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status
import play.api.mvc.{AnyContent, Call}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{EmailVerificationConnector, EscConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{ExchangeRateCache, RemovedSubsidyRepository, UndertakingCache}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.IntegrationCommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{EscTestWireMock, IntegrationBaseSpec}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationServiceSpec
    extends IntegrationBaseSpec
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar
    with ScalaFutures
    with DefaultAwaitTimeout
    with WireMockSupport {

  private val escTestWireMock = new EscTestWireMock(wireMockServer)

  private val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  private val mockServicesConfig = mock[ServicesConfig]

  private val nextPageUrl = "/next-page-url"
  private val previousPage = Call(GET, "/previous-page-url")

  private val responseBody =
    s"""
       |{
       |  "redirectUri": "$nextPageUrl"
       |}""".stripMargin

  private val unverifiedVerificationRequest = VerifiedEmail("unverified@something.com", "someId", verified = false)
  private val verifiedVerificationRequest = VerifiedEmail("verified@something.com", "someId", verified = true)

  private val configBaseUrlKey = "email-verification-frontend"
  "EmailVerificationService" when {

    "getEmailVerification is called" must {

      "return none when the backend service returns 404" in {
        //repository.put(eori1, unverifiedVerificationRequest).futureValue.id shouldBe eori1

        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          escTestWireMock.getEmailVerification.stubExpected(404, eori1)
          emailVerificationService.getEmailVerification(eori1).futureValue shouldBe None
          escTestWireMock.getEmailVerification.verify(eori1)
        }

      }

      "return verified" in {
        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          val verifiedEmailJson = VerifiedEmail.verifiedEmailFormat.writes(verifiedVerificationRequest).toString()

          escTestWireMock.getEmailVerification
            .stubExpected(statusCode = 200, expectedEoriInUrl = eori1, response = verifiedEmailJson)

          emailVerificationService.getEmailVerification(eori1).futureValue shouldBe Some(verifiedVerificationRequest)
          escTestWireMock.getEmailVerification.verify(eori1)
        }
      }

    }

    def testWithRunningApp[A](f: EmailVerificationService => A): Unit = {
      val app = configuredApplication
      play.api.test.Helpers.running(app) {
        val emailVerificationService = createEmailVerificationService(app.injector.instanceOf[EscConnector])
        f(emailVerificationService)
      }
    }

    def configuredApplication: Application =
      new play.api.inject.guice.GuiceApplicationBuilder()
        .configure(
          "microservice.services.eis.protocol" -> "http",
          "microservice.services.esc.host" -> "localhost",
          "microservice.services.esc.port" -> wireMockServer.port()
        )
        .build()

    def createEmailVerificationService(escConnector: EscConnector): EmailVerificationService = {

      val undertakingCache = mock[UndertakingCache]

      val escService =
        new EscService(escConnector, undertakingCache, mock[ExchangeRateCache], mock[RemovedSubsidyRepository])
      new EmailVerificationService(mockEmailVerificationConnector, escService, mockServicesConfig)
    }

    "approveVerificationRequest is called" must {

      "report success for a successful update" in {
        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          val expectedEmailByVerificationIdRequest =
            ApproveEmailByVerificationIdRequest(eori = eori1, verificationId = "pending")

          val verifiedEmailResponse =
            VerifiedEmail.verifiedEmailFormat
              .writes(VerifiedEmail(email = "", verificationId = "", verified = true))
              .toString

          escTestWireMock.approveEmailByVerificationId
            .stubExpected(statusCode = 200, response = verifiedEmailResponse)

          emailVerificationService.approveVerificationRequest(eori1, "pending").futureValue shouldBe true
          escTestWireMock.approveEmailByVerificationId.verify(
            expectedEmailByVerificationIdRequest
          )
        }
      }
    }

    "addVerifiedEmail is called" must {

      "store a new email verification request and mark it as verified" in {
        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          val email = "foo@example.com"

          val nonVerifiedEmail = VerifiedEmail(email = "", verificationId = "", verified = false)

          escTestWireMock.startVerification.stubExpected(200, nonVerifiedEmail.asJson.toString)
          escTestWireMock.approveEmailByEori.stubExpected(200, nonVerifiedEmail.copy(verified = true).asJson.toString)

          emailVerificationService.addVerifiedEmail(eori4, email).futureValue shouldBe true

          escTestWireMock.startVerification.verify(StartEmailVerificationRequest(eori4, email))
          escTestWireMock.approveEmailByEori.verify(ApproveEmailAsVerifiedByEoriRequest(eori4))
        }
      }

    }

    "makeVerificationRequest is called" must {
      val nonVerifiedEmail = VerifiedEmail(email = "", verificationId = "", verified = false)
      val credId = "SomeAuthorityId"
      implicit val request: AuthenticatedEnrolledRequest[AnyContent] = AuthenticatedEnrolledRequest(
        authorityId = credId,
        groupId = "SomeGroupId",
        request = FakeRequest(GET, "/"),
        eoriNumber = eori1
      )

      val email = "foo@example.com"

      val createVerifyEmailRequest = EmailVerificationRequest.createVerifyEmailRequest(
        credId,
        "http://localhost/next-page-url",
        email,
        "http://localhost/previous-page-url"
      )

      "redirect to the next page if the email verification succeeded" in {

        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          Mockito
            .when(
              mockEmailVerificationConnector.verifyEmail(
                createVerifyEmailRequest
              )
            )
            // CREATED is important as any other defaults to None at the moment
            .thenReturn(Future.successful(Right(HttpResponse(Status.CREATED, responseBody))))

          Mockito
            .when(mockServicesConfig.baseUrl(configBaseUrlKey))
            .thenReturn("https://site")

          escTestWireMock.startVerification.stubExpected(200, nonVerifiedEmail.asJson.toString)

          val eventualResult = emailVerificationService.makeVerificationRequestAndRedirect(
            email,
            previousPage,
            _ => nextPageUrl
          )

          //Need to block before verify, this verification will inform better than the following assertions on failure
          eventualResult.futureValue
          escTestWireMock.startVerification.verify(StartEmailVerificationRequest.apply(request.eoriNumber, email))

          status(eventualResult) shouldBe SEE_OTHER

          //What it was original but it was all heavily faked
          //redirectLocation(eventualResult) shouldBe Some(request)
          redirectLocation(eventualResult) shouldBe Some("https://site/next-page-url")
        }

      }

      "redirect to the previous page if the email verification failed" in {
        testWithRunningApp { emailVerificationService: EmailVerificationService =>
          val email = "foo@example.com"

          Mockito
            .when(mockServicesConfig.baseUrl(configBaseUrlKey))
            .thenReturn("https://site")

          escTestWireMock.startVerification.stubExpected(200, nonVerifiedEmail.asJson.toString)

          Mockito
            .when(
              mockEmailVerificationConnector.verifyEmail(
                EmailVerificationRequest.createVerifyEmailRequest(
                  credId,
                  "http://localhost/next-page-url",
                  email,
                  "http://localhost/previous-page-url"
                )
              )
            )
            // CREATED is important as any other defaults to None at the moment
            .thenReturn(Future.successful(Right(HttpResponse(Status.BAD_REQUEST, responseBody))))

          val eventualResult = emailVerificationService.makeVerificationRequestAndRedirect(
            email = email,
            previousPage = previousPage,
            nextPageUrl = _ => nextPageUrl
          )

          escTestWireMock.serveEvents shouldBe List.empty

          status(eventualResult) shouldBe SEE_OTHER
          redirectLocation(eventualResult) shouldBe Some(previousPage.url)
        }

      }

    }

  }
}
