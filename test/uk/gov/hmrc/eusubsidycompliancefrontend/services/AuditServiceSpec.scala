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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.mvc.{Headers, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.TermsAndConditionsAccepted
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends Matchers with AnyWordSpecLike with MockFactory {

  private val mockAuditConnector = mock[AuditConnector]

  private def mockSendExtendedEvent(expectedEvent: ExtendedDataEvent)(result: Future[AuditResult]) =
    (mockAuditConnector
      .sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(where { case (actualEvent, _, _) =>
        actualEvent.auditType === expectedEvent.auditType
        actualEvent.auditSource === expectedEvent.auditSource
        actualEvent.detail === expectedEvent.detail
        actualEvent.tags === expectedEvent.tags

      })
      .returning(result)

  private val service = new AuditService(mockAuditConnector)

  "AuditService" when {

    "handling requests to audit an event" must {

      "return successfully" when {

        val requestUri = "/uri"

        implicit val request: Request[_] = FakeRequest(GET, requestUri, Headers(), "")

        implicit val hc: HeaderCarrier = HeaderCarrier()

        val auditEvent = TermsAndConditionsAccepted(eori1)

        val extendedDataEvent = ExtendedDataEvent(
          auditSource = "eu-subsidy-compliance-frontend",
          auditType = auditEvent.auditType,
          detail = Json.toJson(auditEvent),
          tags = hc.toAuditTags(auditEvent.transactionName, requestUri)
        )

        "a 'Successful' AuditResult is given" in {
          mockSendExtendedEvent(extendedDataEvent)(AuditResult.Success.toFuture)

          service.sendEvent(auditEvent) shouldBe (())
        }

        "a 'Disabled' audit result is given" in {
          mockSendExtendedEvent(extendedDataEvent)(AuditResult.Disabled.toFuture)

          service.sendEvent(auditEvent) shouldBe (())
        }

        "a 'Failure' audit result is given" in {
          mockSendExtendedEvent(extendedDataEvent)(AuditResult.Failure("").toFuture)

          service.sendEvent(auditEvent) shouldBe (())
        }

        "the call to audit fails" in {
          mockSendExtendedEvent(extendedDataEvent)(Future.failed(new Exception("")))

          service.sendEvent(auditEvent) shouldBe (())
        }

      }

    }

  }

}
