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

package uk.gov.hmrc.eusubsidycompliancefrontend.connectors

import uk.gov.hmrc.eusubsidycompliancefrontend.models.{SubsidyUpdate, VerifyEmailRequest, VerifyEmailResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EmailVerificationConnector @Inject()(
override protected val http: HttpClient,
  servicesConfig: ServicesConfig
  )(implicit ec: ExecutionContext)
  extends Connector {

  private val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")

  private val verifyEmailUrl = s"$emailVerificationBaseUrl/email-verification/verify-email"

  def useAbsoluteUrls: Boolean = emailVerificationBaseUrl.contains("localhost")


  def verifyEmail(request: VerifyEmailRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): ConnectorResult = {
    makeRequest(
      _.POST[VerifyEmailRequest, HttpResponse](
        verifyEmailUrl,
        request
      )
    )
  }

  def getVerificationJourney(redirectUri: String): String = if(useAbsoluteUrls) "http://localhost:9890" + redirectUri else redirectUri
}
