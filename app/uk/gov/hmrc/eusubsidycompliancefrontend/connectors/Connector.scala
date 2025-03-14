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

import play.api.Logging
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.Connector.ConnectorSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

//Probably better if this was not a trait and injected as a class. Bouncing up and down vertically is not very fun.
//Traits are still inheritance, easy to start with, not so easy to live with as. "Replace Inheritance with Delegation"
//is a refactoring that requires a greater amount of skill than creating the problem it solves so best not create the
//problem.
trait Connector extends Logging {
  type ConnectorResult = Future[Either[ConnectorError, HttpResponse]]

  protected val http: HttpClientV2

  protected def makeRequest(
    request: HttpClientV2 => Future[HttpResponse]
  )(implicit ec: ExecutionContext): ConnectorResult =
    request(http)
      .map { response =>
        if (response.status.isSuccess) {
          Right(response)
        } else {
          Left(
            ConnectorError(
              UpstreamErrorResponse(
                s"Unexpected response - got HTTP ${response.status} with body: ${response.body}",
                response.status
              )
            )
          )
        }
      }
      .recover(t => Left(ConnectorError(t)))
}

object Connector {

  object ConnectorSyntax {
    implicit class ResponseStatusOps(val status: Int) extends AnyVal {
      def isSuccess: Boolean = status >= 200 && status < 300
    }
  }

}
