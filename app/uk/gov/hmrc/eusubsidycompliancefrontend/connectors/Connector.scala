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

import play.api.libs.json.Writes
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.Connector.ConnectorSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

//Probably better if this was not a trait and injected as a class. Bouncing up and down vertically is not very fun.
//Traits are still inheritance, easy to start with, not so easy to live with as. "Replace Inheritance with Delegation"
//is a refactoring that requires a greater amount of skill than creating the problem it solves so best not create the
//problem.
trait Connector extends TracedLogging {

  //Need to write a test for what this actually affects as no tests fail when removed by frontend no longer works
  import uk.gov.hmrc.http.HttpReads.Implicits._
  //  import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

  type ConnectorResult = Future[Either[ConnectorError, HttpResponse]]

  protected val http: HttpClient

  protected def makeRequest(
    request: HttpClient => Future[HttpResponse]
  )(implicit ec: ExecutionContext): ConnectorResult =
    request(http)
      .map({ r: HttpResponse =>
        if (r.status.isSuccess) {
          Right(r)
        } else {
          Left(
            ConnectorError(
              UpstreamErrorResponse(s"Unexpected response - got HTTP ${r.status} with body: ${r.body}", r.status)
            )
          )
        }
      })
      .recover({ case e: Exception =>
        Left(ConnectorError(e))
      })

  private val className = getClass.getSimpleName

  /**
    * Connector hides everything so we have wrap everything bit by bit
    */
  protected def logPost[A](
    methodName: String,
    url: String,
    payload: A
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, wts: Writes[A]): ConnectorResult = {
    logger.info(s"$className.$methodName - posting to $url")
    makeRequest(_.POST[A, HttpResponse](url, payload)).map {
      case Left(error) =>
        logger.error(s"$className.$methodName - failed POST to $url", error)
        Left(error)
      case Right(successResponse: HttpResponse) =>
        logger.info(
          s"$className.$methodName - successful POST to $url"
        )
        Right(successResponse)
    }
  }

  protected def logGet(
    methodName: String,
    url: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ConnectorResult = {
    logger.info(s"EscConnector.$methodName - getting from $url")
    makeRequest(_.GET[HttpResponse](url)).map {
      case Left(error) =>
        logger.error(s"EscConnector.$methodName - failed GET from $url")
        Left(error)
      case Right(successResponse: HttpResponse) =>
        logger.info(s"EscConnector.$methodName - successful GET from $url")

        Right(successResponse)
    }
  }

}

object Connector {

  object ConnectorSyntax {
    implicit class ResponseStatusOps(val status: Int) extends AnyVal {
      def isSuccess: Boolean = status >= 200 && status < 300
    }
  }

}
