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

package uk.gov.hmrc.eusubsidycompliancefrontend.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.eusubsidycompliancefrontend.models.EmailVerificationRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailVerificationStatusResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (
  override protected val http: HttpClientV2,
  servicesConfig: ServicesConfig
) extends Connector {

  lazy private val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")

  lazy private val verifyEmailUrl = url"$emailVerificationBaseUrl/email-verification/verify-email"

  def verifyEmail(
    request: EmailVerificationRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ConnectorResult = {
    makeRequest(
      _.post(verifyEmailUrl)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
    )
  }

  def getVerificationStatus(
    credId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailVerificationStatusResponse] =
    http
      .get(url"$emailVerificationBaseUrl/email-verification/verification-status/$credId")
      .execute[EmailVerificationStatusResponse]
}
