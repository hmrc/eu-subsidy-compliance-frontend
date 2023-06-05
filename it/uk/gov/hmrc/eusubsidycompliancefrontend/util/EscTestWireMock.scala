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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.{ServeEvent, StubMapping}
import play.api.libs.json.Writes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ApproveEmailAsVerifiedByEoriRequest, ApproveEmailByVerificationIdRequest, BusinessEntity, StartEmailVerificationRequest, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingCreate}

import java.time.LocalDate
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
  * Ring fence this into a variable as loose methods in tests from traits just turns in to a brute force memory
  * exercise that eats velocity when writing tests.
  */
class EscTestWireMock(wireMockServer: WireMockServer, baseUrl: String = "/eu-subsidy-compliance") {

  private implicit class MappingBuilderOps(responseDefinitionBuilder: ResponseDefinitionBuilder) {
    def withJsonHeader: ResponseDefinitionBuilder = {
      responseDefinitionBuilder.withHeader("content-type", "application/json")

      responseDefinitionBuilder
    }
  }

  //language=json - connector tests don't care about response structure but the more detailed integration tests do
  val defaultResponse: String =
    """
      |{
      |  "key" : "value"
      |}
      |""".stripMargin

  def serveEvents: List[ServeEvent] = wireMockServer.getAllServeEvents.asScala.toList

  object createUndertaking {
    private val url = s"$baseUrl/undertaking"
    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(undertakingCreate: UndertakingCreate)(implicit writes: Writes[UndertakingCreate]): Unit =
      wireMockServer.verify(
        createPostVerification(url, undertakingCreate, writes)
      )
  }

  private def createPostExpectation(url: String, statusCode: Int, response: String = defaultResponse): StubMapping = {
    WireMock
      .post(url)
      .willReturn(
        WireMock.aResponse().withStatus(statusCode).withJsonHeader.withBody(response)
      )
      .build()
  }

  private def createPostVerification[A](
    url: String,
    undertakingCreate: A,
    writes: Writes[A]
  ): RequestPatternBuilder = {
    WireMock
      .postRequestedFor(WireMock.urlEqualTo(url))
      .withHeader("content-type", WireMock.equalTo("application/json"))
      .withRequestBody(WireMock.equalToJson(writes.writes(undertakingCreate).toString()))
  }

  object updateUndertaking {
    private val url = s"$baseUrl/undertaking/update"

    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(undertaking: Undertaking)(implicit writes: Writes[Undertaking]): Unit =
      wireMockServer.verify(
        createPostVerification(url, undertaking, writes)
      )
  }

  object disableUndertaking {
    private val url = s"$baseUrl/undertaking/disable"

    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(undertaking: Undertaking)(implicit writes: Writes[Undertaking]): Unit =
      wireMockServer.verify(
        createPostVerification(url, undertaking, writes)
      )
  }

  object addMember {
    private val urlFormat = s"$baseUrl/undertaking/member/%s"

    def stubExpected(statusCode: Int, undertakingRef: UndertakingRef): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(urlFormat.format(undertakingRef), statusCode)
      )
    }

    def verify(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
      writes: Writes[BusinessEntity]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(urlFormat.format(undertakingRef), businessEntity, writes)
      )
  }

  object removeMember {
    private val urlFormat = s"$baseUrl/undertaking/member/remove/%s"

    def stubExpected(statusCode: Int, undertakingRef: UndertakingRef): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(urlFormat.format(undertakingRef), statusCode)
      )
    }

    def verify(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
      writes: Writes[BusinessEntity]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(urlFormat.format(undertakingRef), businessEntity, writes)
      )
  }

  object createSubsidy {
    private val url = s"$baseUrl/subsidy/update"

    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(subsidyUpdate: SubsidyUpdate)(implicit
      writes: Writes[SubsidyUpdate]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(url, subsidyUpdate, writes)
      )
  }

  object removeSubsidy {
    private val url = s"$baseUrl/subsidy/update"

    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(subsidyUpdate: SubsidyUpdate)(implicit
      writes: Writes[SubsidyUpdate]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(url, subsidyUpdate, writes)
      )
  }

  object retrieveSubsidy {
    private val url = s"$baseUrl/subsidy/retrieve"

    def stubExpected(statusCode: Int): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode)
      )
    }

    def verify(subsidyRetrieve: SubsidyRetrieve)(implicit writes: Writes[SubsidyRetrieve]): Unit =
      wireMockServer.verify(
        createPostVerification(url, subsidyRetrieve, writes)
      )
  }

  object approveEmailByEori {
    private val url = s"$baseUrl/email/approve/eori"

    def stubExpected(statusCode: Int, response: String = defaultResponse): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode, response)
      )
    }

    def verify(approveEmailAsVerifiedByEoriRequest: ApproveEmailAsVerifiedByEoriRequest)(implicit
      writes: Writes[ApproveEmailAsVerifiedByEoriRequest]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(url, approveEmailAsVerifiedByEoriRequest, writes)
      )
  }

  object startVerification {
    private val url = s"$baseUrl/email/start-verification"

    def stubExpected(statusCode: Int, response: String = defaultResponse): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode, response)
      )
    }

    def verify(startEmailVerificationRequest: StartEmailVerificationRequest)(implicit
      writes: Writes[StartEmailVerificationRequest]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(url, startEmailVerificationRequest, writes)
      )
  }

  object approveEmailByVerificationId {
    private val url = s"$baseUrl/email/approve/verification-id"

    def stubExpected(statusCode: Int, response: String = defaultResponse): Unit = {
      wireMockServer.addStubMapping(
        createPostExpectation(url, statusCode, response)
      )
    }

    def verify(approveEmailByVerificationIdRequest: ApproveEmailByVerificationIdRequest)(implicit
      writes: Writes[ApproveEmailByVerificationIdRequest]
    ): Unit =
      wireMockServer.verify(
        createPostVerification(url, approveEmailByVerificationIdRequest, writes)
      )
  }

  object retrieveUndertaking {
    private val urlFormat = s"$baseUrl/undertaking/%s"

    def stubExpected(statusCode: Int, eori: EORI): Unit = {
      wireMockServer.addStubMapping(
        createGetExpectation(urlFormat.format(eori), statusCode)
      )
    }

    def verify(eori: EORI): Unit =
      wireMockServer.verify(
        createGetVerification(urlFormat.format(eori))
      )
  }

  private def createGetExpectation[A](url: String, statusCode: Int, response: String = defaultResponse): StubMapping = {
    WireMock
      .get(url)
      .willReturn(
        WireMock.aResponse().withStatus(statusCode).withJsonHeader.withBody(response)
      )
      .build()
  }

  private def createGetVerification[A](url: String): RequestPatternBuilder = {
    WireMock
      .getRequestedFor(WireMock.urlEqualTo(url))
  }

  object retrieveExchangeRate {
    private val urlFormat = s"$baseUrl/exchangerate/%s"

    def stubExpected(statusCode: Int, localDate: LocalDate): Unit = {
      wireMockServer.addStubMapping(
        createGetExpectation(urlFormat.format(localDate), statusCode)
      )
    }

    def verify(localDate: LocalDate): Unit =
      wireMockServer.verify(
        createGetVerification(urlFormat.format(localDate))
      )
  }

  object getEmailVerification {
    private val urlFormat = s"$baseUrl/email/verification-status/%s"

    def stubExpected(statusCode: Int, expectedEoriInUrl: EORI, response: String = defaultResponse): Unit = {
      wireMockServer.addStubMapping(
        createGetExpectation(url = urlFormat.format(expectedEoriInUrl), statusCode = statusCode, response = response)
      )
    }

    def verify(eori: EORI): Unit =
      wireMockServer.verify(
        createGetVerification(urlFormat.format(eori))
      )
  }
}
