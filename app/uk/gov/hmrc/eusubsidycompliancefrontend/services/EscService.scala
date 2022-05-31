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

import cats.data.EitherT
import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import com.google.inject.{Inject, Singleton}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Reads
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.UndertakingCache
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscService @Inject() (
  escConnector: EscConnector,
  undertakingCache: UndertakingCache
)(implicit ec: ExecutionContext) {

  def createUndertaking(undertaking: UndertakingCreate)(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingRef] =
    escConnector
      .createUndertaking(undertaking)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "create undertaking").toFuture
          _ <- undertakingCache.put[Undertaking](eori, undertaking.toUndertakingWithRef(ref))
        } yield ref
      }

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .updateUndertaking(undertaking)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "update undertaking").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
        } yield ref
      }

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] =
    undertakingCache
      .get[Undertaking](eori)
      .toContext
      .orElseF {
        retrieveUndertakingAndHandleErrors(eori).flatMap {
          case Right(Some(undertaking)) =>
            undertakingCache
              .put[Undertaking](eori, undertaking)
              .map(_ => undertaking.some)
          case Right(None) => Option.empty[Undertaking].toFuture
          case Left(ex) => Future.failed[Option[Undertaking]](ex)
        }
      }
      .value

  def retrieveUndertakingAndHandleErrors(
    eori: EORI
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Option[Undertaking]]] = {

    def parseResponse(response: HttpResponse) =
      response
        .parseJSON[Undertaking]
        .map(j => Right(j.some))
        .getOrElse(sys.error("Error parsing Undertaking in ESC response"))

    EitherT(escConnector.retrieveUndertaking(eori))
      .flatMapF(r => parseResponse(r).toFuture)
      .recover { case ConnectorError(_, WithStatusCode(NOT_FOUND)) =>
        None
      }
      .value
  }

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector
      .addMember(undertakingRef, businessEntity)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "add member").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
        } yield ref
      }

  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector
      .removeMember(undertakingRef, businessEntity)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "add member").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
        } yield ref
      }

  def createSubsidy(subsidyUpdate: SubsidyUpdate)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .createSubsidy(subsidyUpdate)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "add member").toFuture
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
          _ <- undertakingCache.deleteUndertaking(ref)
        } yield ref
      }

  def retrieveSubsidy(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    undertakingCache
      .get[UndertakingSubsidies](eori)
      .toContext
      .getOrElseF {
        escConnector
          .retrieveSubsidy(subsidyRetrieve)
          .flatMap { response =>
            val result: UndertakingSubsidies = handleResponse[UndertakingSubsidies](response, "subsidy retrieve")
            undertakingCache.put[UndertakingSubsidies](eori, result)
          }
      }

  def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector
      .removeSubsidy(undertakingRef, nonHmrcSubsidy)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "remove subsidy").toFuture
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
        } yield ref
      }

  private def handleResponse[A](r: Either[ConnectorError, HttpResponse], action: String)(implicit reads: Reads[A]): A =
    r.fold(
      _ => sys.error(s"Error executing $action"),
      response =>
        if (response.status =!= OK) sys.error(s"Error executing $action - Got response status: ${response.status}")
        else
          response
            .parseJSON[A]
            .getOrElse(sys.error(s"Error parsing response for $action"))
    )
}
