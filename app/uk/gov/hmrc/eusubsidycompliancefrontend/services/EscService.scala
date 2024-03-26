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

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.UndertakingCache
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.{HttpResponseOps, ResponseParsingLogger}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class EscService @Inject() (
  escConnector: EscConnector,
  undertakingCache: UndertakingCache
)(implicit ec: ExecutionContext)
    extends Logging {

  private implicit class LogFutureOps[A](eventualResult: Future[A]) {
    def logResult(successCall: A => String, errorMessage: => String): Future[A] = {
      eventualResult.failed.foreach(logger.error(errorMessage, _))

      eventualResult.map { result =>
        val successMessage = successCall(result)
        logger.info(successMessage)

        result
      }
    }
  }

  def createUndertaking(
    undertakingCreate: UndertakingCreate
  )(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .createUndertaking(undertakingCreate)
      .flatMap { response =>
        handleResponse[UndertakingRef](response, "create undertaking").toFuture
      }
      .logResult(
        successCall = (undertakingRef: UndertakingRef) => s"createUndertaking undertakingRef:$undertakingRef",
        errorMessage = s"createUndertaking failed for UndertakingCreate"
      )

  private def handleResponse[A](r: Either[ConnectorError, HttpResponse], action: String)(implicit
    reads: Reads[A],
    classTag: ClassTag[A] // is used but can appear hidden
  ): A = {
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

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .updateUndertaking(undertaking)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "update undertaking").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
        } yield ref
      }
      .logResult(
        successCall = (undertakingRef: UndertakingRef) => s"updateUndertaking undertaking reference $undertakingRef",
        errorMessage = s"updateUndertaking failed for ${undertaking.reference}"
      )

  def disableUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .disableUndertaking(undertaking)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "disable undertaking").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
        } yield ref
      }
      .logResult(
        successCall = (undertaking: UndertakingRef) => s"disableUndertaking undertaking $undertaking",
        errorMessage = s"disableUndertaking failed for Undertaking:$undertaking"
      )

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] = {
    def retrieve(eoriNumber: EORI) = retrieveUndertakingAndHandleErrors(eoriNumber).flatMap {
      case Right(Some(undertaking)) =>
        undertakingCache
          .put[Undertaking](eori, undertaking)
          .map(_ => undertaking.some)
      case Right(None) => Option.empty[Undertaking].toFuture
      case Left(ex) => Future.failed[Option[Undertaking]](ex)
    }

    undertakingCache.get[Undertaking](eori).flatMap {
      case Some(undertaking) => Future.successful(Some(undertaking))
      case None => retrieve(eori)
    }
  }

  def retrieveUndertakingAndHandleErrors(
    eori: EORI
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Option[Undertaking]]] = {
    val eitherLogger = new ResponseParsingLogger[ConnectorError, Undertaking] {
      override def logSuccess(undertaking: Undertaking): Unit =
        logger.info(s"retrieveUndertakingAndHandleErrors: Successfully received undertaking for EORI:$eori")

      override def logValidationFailure(
        response: HttpResponse,
        validationFailures: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
      ): Unit =
        logger.error(
          s"retrieveUndertakingAndHandleErrors: Failed validation for eori:$eori response with $validationFailures"
        )

      override def logParsingFailure(response: HttpResponse, throwable: Throwable): Unit =
        logger.error(
          s"retrieveUndertakingAndHandleErrors: Failed parsing for eori:$eori to Undertaking",
          throwable
        )
    }

    implicit val maybeLogger: Option[ResponseParsingLogger[ConnectorError, Undertaking]] = Some(eitherLogger)

    def parseResponse(response: HttpResponse): Right[Nothing, Option[Undertaking]] =
      response
        .parseJSON[Undertaking]
        .map(j => Right(j.some))
        .getOrElse {
          sys.error(s"Error parsing EORI:$eori Undertaking from ESC")
        }

    escConnector.retrieveUndertaking(eori).map {
      case Right(response) => parseResponse(response)
      case Left(ConnectorError(_, WithStatusCode(NOT_FOUND))) => Right(None)
      case Left(err) => Left(err)
    }
  }

  def getUndertakingBalance(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[UndertakingBalance]] = {
    escConnector.getUndertakingBalance(eori)
  }

  def getExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): Future[Option[MonthlyExchangeRate]] = {
    escConnector.getExchangeRate(date)
  }

  def getUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Undertaking] =
    retrieveUndertaking(eori).toContext
      .getOrElse(throw new IllegalStateException("Expected undertaking not found"))
      .logResult(
        successCall = (undertaking: Undertaking) => s"getUndertaking $undertaking for EORI '$eori'",
        errorMessage = s"Failed getUndertaking for EORI '$eori'"
      )

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
      .logResult(
        successCall = (undertaking: UndertakingRef) =>
          s"addMember returned undertaking '$undertaking' for undertakingRef '$undertakingRef' and $businessEntity",
        errorMessage = s"addMember failed for undertakingRef '$undertakingRef' businessEntity '$businessEntity''"
      )

  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector
      .removeMember(undertakingRef, businessEntity)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "add member").toFuture
          _ <- undertakingCache.deleteUndertaking(ref)
        } yield ref
      }
      .logResult(
        successCall = (undertaking: UndertakingRef) =>
          s"removeMember UndertakingRef:$undertakingRef, " +
            s"BusinessEntity:$businessEntity returned $undertaking",
        errorMessage = s"Failed removeMember UndertakingRef:$undertakingRef, BusinessEntity:$businessEntity"
      )

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
      .logResult(
        successCall = undertakingRef => s"createSubsidy returned UndertakingRef:$undertakingRef",
        errorMessage = s"Failed to createSubsidy for undertakingRef:${subsidyUpdate.undertakingIdentifier}"
      )

  def retrieveAllSubsidies(
    undertakingRef: UndertakingRef
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    retrieveSubsidies(SubsidyRetrieve(undertakingRef, Option.empty))
      .logResult(
        successCall = (undertakingSubsidies: UndertakingSubsidies) =>
          s"retrieveAllSubsidies UndertakingRef:$undertakingRef returned $undertakingSubsidies",
        errorMessage = s"retrieveAllSubsidies failed for UndertakingRef:$undertakingRef"
      )

  private def retrieveSubsidies(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    undertakingCache
      .get[UndertakingSubsidies](eori)
      .toContext
      .getOrElseF {
        escConnector
          .retrieveSubsidy(subsidyRetrieve)
          .flatMap { response =>
            undertakingCache
              .put[UndertakingSubsidies](eori, handleResponse[UndertakingSubsidies](response, "subsidy retrieve"))
          }
      }

  def retrieveSubsidiesForDateRange(
    undertakingRef: UndertakingRef,
    dateRange: (LocalDate, LocalDate)
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    retrieveSubsidies(SubsidyRetrieve(undertakingRef, dateRange.some))
      .logResult(
        successCall =
          (_: UndertakingSubsidies) => s"retrieveSubsidiesForDateRange UndertakingRef:$undertakingRef succeeded",
        errorMessage = s"retrieveSubsidiesForDateRange failed for UndertakingRef:$undertakingRef"
      )

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .removeSubsidy(undertakingRef, nonHmrcSubsidy)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "remove subsidy").toFuture
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
        } yield ref
      }
      .logResult(
        successCall = (undertaking: UndertakingRef) =>
          s"removeSubsidy UndertakingRef:$undertakingRef, NonHmrcSubsidy:$nonHmrcSubsidy returned UndertakingRef:$undertaking",
        errorMessage = s"removeSubsidy failed UndertakingRef:$undertakingRef, NonHmrcSubsidy:$nonHmrcSubsidy"
      )

  def clearUndertakingCache(ref: UndertakingRef): Future[Unit] =
    for {
      _ <- undertakingCache.deleteUndertaking(ref)
      _ <- undertakingCache.deleteUndertakingSubsidies(ref)
    } yield ()

}
