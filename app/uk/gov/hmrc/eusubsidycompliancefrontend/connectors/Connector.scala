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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.http.{HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import Connector.ConnectorSyntax._

trait Connector {

  type ConnectorResult = Future[Either[ConnectorError, HttpResponse]]

  protected val http: HttpClient

  protected def makeRequest(
    request: HttpClient => Future[HttpResponse]
  )(implicit ec: ExecutionContext): ConnectorResult =
    request(http) map { r: HttpResponse =>
      if (r.status.isSuccess) Right(r)
      else Left(ConnectorError(UpstreamErrorResponse(s"Unexpected response - got HTTP ${r.status}", r.status)))
    } recover { case e: Exception =>
      Left(ConnectorError(e))
    }

}

object Connector {

  object ConnectorSyntax {
    implicit class ResponseStatusOps(val status: Int) extends AnyVal {
      def isSuccess: Boolean = status >= 200 && status < 300
    }
  }

}
