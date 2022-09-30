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

import org.mongodb.scala.model.Filters
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{AnyContent, Call}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailVerificationRequest, VerifiedEmail}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class EmailVerificationServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory
  with ScalaFutures with DefaultAwaitTimeout with DefaultPlayMongoRepositorySupport[CacheItem] {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  override protected def repository = new EoriEmailDatastore(mongoComponent)

  private val mockServicesConfig = mock[ServicesConfig]

  private val service = new EmailVerificationService(
    mockEmailVerificationConnector,
    repository,
    mockServicesConfig,
  )

  override def afterAll(): Unit = repository.collection.deleteMany(filter = Filters.exists("_id"))

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

  private val unverifiedVerificationRequest = VerifiedEmail("unverified@something.com", "someId", verified = false)
  private val verifiedVerificationRequest = VerifiedEmail("verified@something.com", "someId", verified = true)

  "EmailVerificationService" when {

    "getEmailVerification is called" must {

        "return none" in {
          repository.put(eori1, unverifiedVerificationRequest).futureValue.id shouldBe eori1
          service.getEmailVerification(eori1).futureValue shouldBe None
        }

        "return verified" in {
          repository.put(eori2, verifiedVerificationRequest) .futureValue.id shouldBe eori2
          service.getEmailVerification(eori2).futureValue should contain(verifiedVerificationRequest)
        }

    }

    "approveVerificationRequest is called" must {

      "report success for a successful update" in {
        repository.put(eori1, unverifiedVerificationRequest.copy(verificationId = "pending")).futureValue.id shouldBe eori1
        service.approveVerificationRequest(eori1, "pending").futureValue shouldBe true

        service.getEmailVerification(eori1).futureValue should
          contain(unverifiedVerificationRequest.copy(verified = true, verificationId = "pending"))
      }
    }

    "addVerifiedEmail is called" must {

      "store a new email verification request and mark it as verified" in {
        val email = "foo@example.com"

        service.addVerifiedEmail(eori4, email).futureValue shouldBe (())

        // Query mongo to confirm that we have a verified record
        val result = service.getEmailVerification(eori4)

        result.futureValue.map(_.email) should contain(email)
        result.futureValue.map(_.verified) should contain(true)
        result.futureValue.map(_.verificationId.length) should contain(36) // Crude UUID is set check
      }

    }

    "makeVerificationRequest is called" must {

      implicit val request: AuthenticatedEnrolledRequest[AnyContent] = AuthenticatedEnrolledRequest(
        authorityId = "SomeAuthorityId",
        groupId = "SomeGroupId",
        request = FakeRequest(GET, "/"),
        eoriNumber = eori1
      )

      "redirect to the next page if the email verification succeeded" in {
        inSequence {
          mockVerifyEmail()
        }

        val result = service.makeVerificationRequestAndRedirect(
          "foo@example.com",
          previousPage,
          _ => nextPageUrl,
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(nextPageUrl)
      }

      "redirect to the previous page if the email verification failed" in {
        inSequence {
          mockVerifyEmail(status = BAD_REQUEST)
        }

        val result = service.makeVerificationRequestAndRedirect(
          "foo@example.com",
          previousPage,
          _ => nextPageUrl,
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(previousPage.url)
      }

    }

  }
}
