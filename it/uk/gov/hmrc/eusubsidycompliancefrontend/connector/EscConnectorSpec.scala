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

package uk.gov.hmrc.eusubsidycompliancefrontend.connector

import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ApproveEmailAsVerifiedByEoriRequest, ApproveEmailByVerificationIdRequest, BusinessEntity, StartEmailVerificationRequest, SubsidyUpdate, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.IntegrationCommonTestData
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{EscTestWireMock, IntegrationBaseSpec}
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class EscConnectorSpec extends IntegrationBaseSpec with WireMockSupport {

  private val escTestWireMock = new EscTestWireMock(wireMockServer, "/eu-subsidy-compliance")

  private val eori = EORI("GB1233455444333")
  //Don't infect scope with static imports and stops you bouncing off to the middle of nowhere
  private val undertakingRef: UndertakingRef = IntegrationCommonTestData.undertaking.reference
  private val businessEntity: BusinessEntity = IntegrationCommonTestData.businessEntity3
  private val nonHmrcSubsidy = IntegrationCommonTestData.nonHmrcSubsidy
  private val subsidyUpdateForDeletion: SubsidyUpdate =
    SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy)

  private val subsidyRetrieve = IntegrationCommonTestData.subsidyRetrieve
  private val currentLocalDate = LocalDate.now()
  private val testEmailAddress = "email@test.com"
  private val verificationId = UUID.randomUUID().toString

  implicit class ResponseOps(eventualErrorOrHttpResponse: Future[Either[Throwable, HttpResponse]]) {
    def asTestableValue: Either[Option[Class[_ <: Throwable]], (Int, String)] = {
      eventualErrorOrHttpResponse.futureValue
        .map { httpResponse =>
          httpResponse.status -> httpResponse.body
        }
        .left
        .map { throwable =>
          Option(throwable.getCause).map(_.getClass)

        }
    }
  }

  "EscConnector" can {
    case class EscErrorScenarioSetup[A](
      message: String,
      responseCode: Int,
      expectedErrorCause: Class[A]
    )

    val errorCases = List(
      EscErrorScenarioSetup(
        message = "404 should return Upstream4xxResponse as the cause (correct implicit passed check)",
        responseCode = 404,
        expectedErrorCause = classOf[Upstream4xxResponse]
      ),
      EscErrorScenarioSetup(
        message = "500 should return Upstream4xxResponse as the cause (correct implicit passed check)",
        responseCode = 500,
        expectedErrorCause = classOf[Upstream5xxResponse]
      )
    )

    "createUndertaking should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.createUndertaking.stubExpected(200)
        val writeableUndertaking = IntegrationCommonTestData.writeableUndertaking
        escConnector.createUndertaking(writeableUndertaking).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.createUndertaking.verify(writeableUndertaking)
      }
    }

    def testWithRunningApp[A](f: EscConnector => A): Unit = {
      val app = configuredApplication
      play.api.test.Helpers.running(app) {
        f(app.injector.instanceOf[EscConnector])
      }
    }

    def configuredApplication: Application =
      new GuiceApplicationBuilder()
        .configure(
          "microservice.services.eis.protocol" -> "http",
          "microservice.services.esc.host" -> "localhost",
          "microservice.services.esc.port" -> wireMockServer.port()
        )
        .build()

    //Error cause seems to control behaviour outside. This could be abstracted outside the class
    //but it useful to keep failures firing within the test to limit bouncing around on failure
    //Damp versus dry as well
    "createUndertaking should remap to correct error cause on failure" in {
      val writeableUndertaking = IntegrationCommonTestData.writeableUndertaking

      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          //  super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.createUndertaking.stubExpected(errorCase.responseCode)
            escConnector.createUndertaking(writeableUndertaking).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.createUndertaking.verify(writeableUndertaking)
          }
        }
      }
    }

    "updateUndertaking should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.updateUndertaking.stubExpected(200)
        val undertaking = IntegrationCommonTestData.undertaking
        escConnector.updateUndertaking(undertaking).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.updateUndertaking.verify(undertaking)
      }
    }

    "updateUndertaking should remap to correct error cause on failure" in {
      val undertaking: Undertaking = IntegrationCommonTestData.undertaking

      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.updateUndertaking.stubExpected(errorCase.responseCode)
            escConnector.updateUndertaking(undertaking).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.updateUndertaking.verify(undertaking)
          }
        }
      }
    }

    "disableUndertaking should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.disableUndertaking.stubExpected(200)
        val undertaking = IntegrationCommonTestData.undertaking
        escConnector.disableUndertaking(undertaking).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.disableUndertaking.verify(undertaking)
      }
    }

    "disableUndertaking should remap to correct error cause on failure" in {
      val undertaking: Undertaking = IntegrationCommonTestData.undertaking

      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.disableUndertaking.stubExpected(errorCase.responseCode)
            escConnector.disableUndertaking(undertaking).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.disableUndertaking.verify(undertaking)
          }
        }
      }
    }

    "retrieveUndertaking should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.retrieveUndertaking.stubExpected(200, eori)

        escConnector.retrieveUndertaking(eori).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.retrieveUndertaking.verify(eori)
      }
    }

    "retrieveUndertaking should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.retrieveUndertaking.stubExpected(errorCase.responseCode, eori)
            escConnector.retrieveUndertaking(eori).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.retrieveUndertaking.verify(eori)
          }
        }
      }
    }

    "addMember should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.addMember.stubExpected(200, undertakingRef)

        escConnector.addMember(undertakingRef, businessEntity).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.addMember.verify(undertakingRef, businessEntity)
      }
    }

    "addMember should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.addMember.stubExpected(errorCase.responseCode, undertakingRef)
            escConnector.addMember(undertakingRef, businessEntity).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.addMember.verify(undertakingRef, businessEntity)
          }
        }
      }
    }

    "removeMember should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.removeMember.stubExpected(200, undertakingRef)

        escConnector.removeMember(undertakingRef, businessEntity).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.removeMember.verify(undertakingRef, businessEntity)
      }
    }

    "removeMember should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.removeMember.stubExpected(errorCase.responseCode, undertakingRef)
            escConnector.removeMember(undertakingRef, businessEntity).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.removeMember.verify(undertakingRef, businessEntity)
          }
        }
      }
    }

    "createSubsidy should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.createSubsidy.stubExpected(200)
        escConnector.createSubsidy(subsidyUpdateForDeletion).asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.createSubsidy.verify(subsidyUpdateForDeletion)
      }
    }

    "createSubsidy should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.createSubsidy.stubExpected(errorCase.responseCode)
            escConnector.createSubsidy(subsidyUpdateForDeletion).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.createSubsidy.verify(subsidyUpdateForDeletion)
          }
        }
      }
    }

    "removeSubsidy should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.removeSubsidy.stubExpected(200)
        escConnector
          .removeSubsidy(undertakingRef, nonHmrcSubsidy)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.removeSubsidy.verify(subsidyUpdateForDeletion)
      }
    }

    "removeSubsidy should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.removeSubsidy.stubExpected(errorCase.responseCode)
            escConnector.removeSubsidy(undertakingRef, nonHmrcSubsidy).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.removeSubsidy.verify(subsidyUpdateForDeletion)
          }
        }
      }
    }

    "retrieveSubsidy should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.retrieveSubsidy.stubExpected(200)
        escConnector
          .retrieveSubsidy(subsidyRetrieve)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.retrieveSubsidy.verify(subsidyRetrieve)
      }
    }

    "retrieveSubsidy should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.retrieveSubsidy.stubExpected(errorCase.responseCode)
            escConnector.retrieveSubsidy(subsidyRetrieve).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.retrieveSubsidy.verify(subsidyRetrieve)
          }
        }
      }
    }

    "retrieveExchangeRate should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.retrieveExchangeRate.stubExpected(200, currentLocalDate)
        escConnector
          .retrieveExchangeRate(currentLocalDate)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.retrieveExchangeRate.verify(currentLocalDate)
      }
    }

    "retrieveExchangeRate should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.retrieveExchangeRate.stubExpected(errorCase.responseCode, currentLocalDate)
            escConnector.retrieveExchangeRate(currentLocalDate).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.retrieveExchangeRate.verify(currentLocalDate)
          }
        }
      }
    }

    "approveEmailByEori should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.approveEmailByEori.stubExpected(200)
        escConnector
          .approveEmailByEori(eori)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.approveEmailByEori.verify(ApproveEmailAsVerifiedByEoriRequest(eori))
      }
    }

    "approveEmailByEori should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.approveEmailByEori.stubExpected(errorCase.responseCode)
            escConnector.approveEmailByEori(eori).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.approveEmailByEori.verify(ApproveEmailAsVerifiedByEoriRequest(eori))
          }
        }
      }
    }

    "startVerification should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.startVerification.stubExpected(200)
        escConnector
          .startVerification(eori, testEmailAddress)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.startVerification.verify(StartEmailVerificationRequest(eori, testEmailAddress))
      }
    }

    "startVerification should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.startVerification.stubExpected(errorCase.responseCode)
            escConnector.startVerification(eori, testEmailAddress).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.startVerification.verify(StartEmailVerificationRequest(eori, testEmailAddress))
          }
        }
      }
    }

    "approveEmailByVerificationId should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.approveEmailByVerificationId.stubExpected(200)
        escConnector
          .approveEmailByVerificationId(eori, verificationId)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.approveEmailByVerificationId.verify(ApproveEmailByVerificationIdRequest(eori, verificationId))
      }
    }

    "approveEmailByVerificationId should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.approveEmailByVerificationId.stubExpected(errorCase.responseCode)
            escConnector.approveEmailByVerificationId(eori, verificationId).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.approveEmailByVerificationId.verify(
              ApproveEmailByVerificationIdRequest(eori, verificationId)
            )
          }
        }
      }
    }

    "getEmailVerification should handle success" in {
      testWithRunningApp { escConnector: EscConnector =>
        escTestWireMock.getEmailVerification.stubExpected(200, eori)
        escConnector
          .getEmailVerification(eori)
          .asTestableValue shouldBe Right(
          200 -> escTestWireMock.defaultResponse
        )
        escTestWireMock.getEmailVerification.verify(eori)
      }
    }

    "getEmailVerification should remap to correct error cause on failure" in {
      testWithRunningApp { escConnector: EscConnector =>
        errorCases.foreach { errorCase =>
          super.beforeEach()
          withClue(errorCase.message) {
            escTestWireMock.getEmailVerification.stubExpected(errorCase.responseCode, eori)
            escConnector.getEmailVerification(eori).asTestableValue shouldBe Left(
              Some(errorCase.expectedErrorCause)
            )
            escTestWireMock.getEmailVerification.verify(eori)
          }
        }
      }
    }

  }
}
