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
import org.mongodb.scala.model.Filters
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{Json, Writes}
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{VerifiedEmail, EmailVerificationResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class EmailVerificationServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory with ScalaFutures with DefaultAwaitTimeout with DefaultPlayMongoRepositorySupport[CacheItem] {


  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
  implicit val writes: Writes[VerifiedEmail] = Json.writes[VerifiedEmail]

    override protected def repository = new EoriEmailDatastore(mongoComponent)

    private val service = new EmailVerificationService(
    mockEmailVerificationConnector,
    repository
  )
  override def afterAll(): Unit = {
    repository.collection.deleteMany(filter = Filters.exists("_id"))
  }

  val unverifiedVerificationRequest = VerifiedEmail("unverified@something.com", "someId", false)
  val verifiedVerificationRequest = VerifiedEmail("verified@something.com", "someId", true)

  val mockVerifyEmailResponse = EmailVerificationResponse("testRedirectUrl")


  "EmailVerificationService" when {

    "getEmailVerification is called" must {

        "return none" in {
          Await.result(repository.put(eori1, unverifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori1).futureValue shouldBe None
        }

        "return verified" in {
          Await.result(repository.put(eori2, verifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori2).futureValue shouldBe verifiedVerificationRequest.some
        }

    }

    "verifyEori is called" must {

        "record is verified" in {
          Await.result(repository.put(eori3, unverifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori3).futureValue shouldBe None
          service.verifyEori(eori3)
          service.getEmailVerification(eori3).futureValue shouldBe unverifiedVerificationRequest.copy(verified = true).some
        }
    }

    "approveVerificationRequest is called" must {

        "success" in {
          Await.result(repository.put(eori1, unverifiedVerificationRequest.copy(verificationId = "pending")), Duration(10, "seconds"))
          service.approveVerificationRequest(eori1, "pending")
          service.getEmailVerification(eori1).futureValue shouldBe unverifiedVerificationRequest.copy(verified = true, verificationId = "pending").some
        }
    }

    "emailVerificationRedirect is called" must {


        "correct redirect is given" in {
          (mockEmailVerificationConnector
            .getVerificationJourney(_: String))
            .expects(*)
            .returning("redirecturl")
          val a = service.emailVerificationRedirect(mockVerifyEmailResponse.some)
          redirectLocation(a.toFuture) shouldBe "redirecturl".some

      }

        "no redirect is given" in {
          val a = service.emailVerificationRedirect(None)
          redirectLocation(a.toFuture) shouldBe routes.UndertakingController.getConfirmEmail().url.some
        }


    }

    "addVerificationRequest is called" must {

        "add verification record successfully" in {
          val pendingId = Await.result(service.addVerificationRequest(eori4, "testemail@aol.com"), Duration(5, "seconds"))
          service.getEmailVerification(eori4).futureValue shouldBe None
          Await.result(service.verifyEori(eori4), Duration(5, "seconds"))
          service.getEmailVerification(eori4).futureValue shouldBe VerifiedEmail("testemail@aol.com", pendingId, true).some
        }
    }
  }
}
