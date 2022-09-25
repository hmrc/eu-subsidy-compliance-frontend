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
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailVerificationResponse, VerifiedEmail}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

// TODO - expand coverage to ensure API changes are properly covered
class EmailVerificationServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory
  with ScalaFutures with DefaultAwaitTimeout with DefaultPlayMongoRepositorySupport[CacheItem] {

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

    override protected def repository = new EoriEmailDatastore(mongoComponent)

    private val service = new EmailVerificationService(
    mockEmailVerificationConnector,
    repository,
    mock[ServicesConfig]
  )
  override def afterAll(): Unit = {
    repository.collection.deleteMany(filter = Filters.exists("_id"))
  }

  private val unverifiedVerificationRequest = VerifiedEmail("unverified@something.com", "someId", verified = false)
  private val verifiedVerificationRequest = VerifiedEmail("verified@something.com", "someId", verified = true)

  private val mockVerifyEmailResponse = EmailVerificationResponse("testRedirectUrl")

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

    "verifyEori is called" must {

        "record is verified" in {
          repository.put(eori3, unverifiedVerificationRequest).futureValue.id shouldBe eori3

          service.getEmailVerification(eori3).futureValue shouldBe None
          service.verifyEori(eori3).futureValue.id shouldBe eori3

          service.getEmailVerification(eori3).futureValue should contain(unverifiedVerificationRequest.copy(verified = true))
        }
    }

    "approveVerificationRequest is called" must {

        "success" in {
          repository.put(eori1, unverifiedVerificationRequest.copy(verificationId = "pending")).futureValue.id shouldBe eori1
          service.approveVerificationRequest(eori1, "pending").futureValue.wasAcknowledged() shouldBe true

          service.getEmailVerification(eori1).futureValue should
            contain(unverifiedVerificationRequest.copy(verified = true, verificationId = "pending"))
        }
    }

    "emailVerificationRedirect is called" must {


        "correct redirect is given" in {
          (mockEmailVerificationConnector
            .getVerificationJourney(_: String))
            .expects(*)
            .returning("redirecturl")
          // TODO - review call param here and revise to ensure we cover the redirect URL correctly
          val a = service.emailVerificationRedirect(routes.UndertakingController.getConfirmEmail())(mockVerifyEmailResponse.some)
          // TODO - review what this is covering
          redirectLocation(a.toFuture) should contain("redirecturl")

      }

        "no redirect is given" in {
          val a = service.emailVerificationRedirect(routes.UndertakingController.getConfirmEmail())(None)
          redirectLocation(a.toFuture) should contain(routes.UndertakingController.getConfirmEmail().url)
        }


    }

    "addVerificationRequest is called" must {

        "add verification record successfully" in {
          val pendingId = service.addVerificationRequest(eori4, "testemail@aol.com").futureValue
          service.getEmailVerification(eori4).futureValue shouldBe None
          service.verifyEori(eori4).futureValue.id shouldBe eori4

          service.getEmailVerification(eori4).futureValue should contain(VerifiedEmail("testemail@aol.com", pendingId, verified = true))
        }

    }
  }
}
