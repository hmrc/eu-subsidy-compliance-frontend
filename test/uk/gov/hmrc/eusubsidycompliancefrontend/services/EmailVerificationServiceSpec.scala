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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailVerificationState, VerifyEmailResponse}
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
  implicit val writes: Writes[EmailVerificationState] = Json.writes[EmailVerificationState]

    override protected def repository = new EoriEmailDatastore(mongoComponent)

    private val service = new EmailVerificationService(
    mockEmailVerificationConnector,
    repository
  )
  override def afterAll(): Unit = {
    repository.collection.deleteMany(filter = Filters.exists("_id"))
  }

  val unverifiedVerificationRequest = EmailVerificationState("unverified@something.com", Some("someId"), false.some)
  val verifiedVerificationRequest = EmailVerificationState("verified@something.com", Some("someId"), true.some)

  val mockVerifyEmailResponse = VerifyEmailResponse("testRedirectUrl")


  "EmailVerificationService" when {

    "getEmailVerification is called" must {

      "No verification found" when {

        "return none" in {
          Await.result(repository.put(eori1, unverifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori1).futureValue shouldBe None
        }
      }

      "Verification found" when {

        "return verified" in {
          Await.result(repository.put(eori2, verifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori2).futureValue shouldBe verifiedVerificationRequest.some
        }
      }

    }

    "verifyEori is called" must {

      "OK" when {

        "record is verified" in {
          Await.result(repository.put(eori3, unverifiedVerificationRequest), Duration(10, "seconds"))
          service.getEmailVerification(eori3).futureValue shouldBe None
          service.verifyEori(eori3)
          service.getEmailVerification(eori3).futureValue shouldBe unverifiedVerificationRequest.copy(verified = true.some).some
        }

      }
    }

    "approveVerificationRequest is called" must {

      "verify record" when {
        "success" in {
          Await.result(repository.put(eori1, unverifiedVerificationRequest.copy(pendingVerificationId = "pending".some)), Duration(10, "seconds"))
          service.approveVerificationRequest(eori1, "pending")
          service.getEmailVerification(eori1).futureValue shouldBe unverifiedVerificationRequest.copy(verified = true.some, pendingVerificationId = "pending".some).some
        }
      }
    }

    "emailVerificationRedirect is called" must {

      "return success" when {

        "correct redirect is given" in {
          (mockEmailVerificationConnector
            .getVerificationJourney(_: String))
            .expects(*)
            .returning("redirecturl")
          val a = service.emailVerificationRedirect(mockVerifyEmailResponse.some)
          redirectLocation(a.toFuture) shouldBe "redirecturl".some
        }

      }
      "return none" when {

        "no redirect is given" in {
          val a = service.emailVerificationRedirect(None)
          redirectLocation(a.toFuture) shouldBe routes.UndertakingController.getConfirmEmail().url.some
        }

      }

    }

    "addVerificationRequest is called" must {

      "return success" when {

        "add verification record successfully" in {
          val pendingId = Await.result(service.addVerificationRequest(eori4, "testemail@aol.com"), Duration(5, "seconds"))
          service.getEmailVerification(eori4).futureValue shouldBe None
          Await.result(service.verifyEori(eori4), Duration(5, "seconds"))
          service.getEmailVerification(eori4).futureValue shouldBe EmailVerificationState("testemail@aol.com", pendingId.some, true.some).some
        }
      }
    }
  }
}
