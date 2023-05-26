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

import cats.data.EitherT
import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{ExchangeRateCache, RemovedSubsidyRepository, UndertakingCache, YearAndMonth}
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
  undertakingCache: UndertakingCache,
  exchangeRateCache: ExchangeRateCache,
  removedSubsidyRepository: RemovedSubsidyRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  private implicit class LogFutureOps[A](eventualResult: Future[A]) {
    def logResult(successCall: A => String, errorMessage: => String): Future[A] = {
      eventualResult.failed.foreach { error =>
        logger.error(errorMessage, error)
      }

      eventualResult.map { result =>
        val successMessage = successCall(result)
        logger.info(successMessage)

        result
      }
    }
  }

  def createUndertaking(
    undertaking: UndertakingCreate
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingRef] =
    escConnector
      .createUndertaking(undertaking)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "create undertaking").toFuture
          _ <- undertakingCache.put[Undertaking](eori, undertaking.toUndertakingWithRef(ref))
        } yield ref
      }
      .logResult(
        successCall = (undertaking: UndertakingRef) => s"createUndertaking $undertaking",
        errorMessage = s"createUndertaking failed for UndertakingCreate:$undertaking"
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
        successCall = (undertaking: UndertakingRef) => s"updateUndertaking undertaking $undertaking",
        errorMessage = s"updateUndertaking failed for $undertaking"
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
    val eventualEoriRequest = undertakingCache
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

    eventualEoriRequest.logResult(
      (maybeUndertaking: Option[Undertaking]) =>
        maybeUndertaking
          .map { undertaking =>
            s"retrieveUndertaking found undertaking $undertaking for EORI '$eori''"
          }
          .getOrElse {
            s"retrieveUndertaking did not find undertaking for EORI '$eori'"
          },
      s"Failed getting EURI undertaking '$eori'"
    )
  }

  def retrieveUndertakingAndHandleErrors(
    eori: EORI
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Option[Undertaking]]] = {
    val eitherLogger = new ResponseParsingLogger[ConnectorError, Undertaking] {
      override def logSuccess(undertaking: Undertaking): Unit =
        logger.info(
          s"retrieveUndertakingAndHandleErrors: Successfully received undertaking for EORI:$eori with UndertakingName:${undertaking.name}"
        )

      override def logValidationFailure(
        response: HttpResponse,
        validationFailures: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
      ): Unit =
        logger.error(
          s"retrieveUndertakingAndHandleErrors: Failed validation for response ${response.body} with $validationFailures"
        )

      override def logParsingFailure(response: HttpResponse, throwable: Throwable): Unit =
        logger.error(
          s"retrieveUndertakingAndHandleErrors: Failed parsing ${response.body} to Undertaking",
          throwable
        )
    }

    implicit val maybeLogger: Option[ResponseParsingLogger[ConnectorError, Undertaking]] = Some(eitherLogger)

    def parseResponse(response: HttpResponse): Right[Nothing, Option[Undertaking]] =
      response
        .parseJSON[Undertaking]
        .map(j => Right(j.some))
        .getOrElse {
          sys.error(s"Error parsing EORI:$eori Undertaking from ESC response ${response.body}")
        }

    EitherT(escConnector.retrieveUndertaking(eori))
      .flatMapF { httpResponse: HttpResponse =>
        parseResponse(httpResponse).toFuture
      }
      .recover { case ConnectorError(_, WithStatusCode(NOT_FOUND)) =>
        logger.info(s"retrieveUndertakingAndHandleErrors EORI:$eori not found")
        None
      }
      .value
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
        successCall = (undertaking: UndertakingRef) =>
          s"createSubsidy SubsidyUpdate:$subsidyUpdate returned UndertakingRef:$undertaking",
        errorMessage = s"Failed createSubsidy SubsidyUpdate:$subsidyUpdate"
      )

  def retrieveAllSubsidies(
    undertakingRef: UndertakingRef
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    retrieveSubsidies(SubsidyRetrieve(undertakingRef, Option.empty))
      .logResult(
        successCall = (undertakingSubsidies: UndertakingSubsidies) =>
          s"retrieveAllSubsidies UndertakingRef:$undertakingRef " +
            s"returned $undertakingSubsidies",
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
            val result: UndertakingSubsidies = handleResponse[UndertakingSubsidies](response, "subsidy retrieve")
            removedSubsidyRepository
              .getAll(eori)
              .flatMap { subsidies =>
                val updatedResult = result.copy(nonHMRCSubsidyUsage = result.nonHMRCSubsidyUsage ++ subsidies.toList)
                undertakingCache.put[UndertakingSubsidies](eori, updatedResult)
              }
          }
      }

  def retrieveSubsidiesForDateRange(
    undertakingRef: UndertakingRef,
    dateRange: (LocalDate, LocalDate)
  )(implicit hc: HeaderCarrier, eori: EORI): Future[UndertakingSubsidies] =
    retrieveSubsidies(SubsidyRetrieve(undertakingRef, dateRange.some))
      .logResult(
        successCall = (undertakingSubsidies: UndertakingSubsidies) =>
          s"retrieveSubsidiesForDateRange UndertakingRef:$undertakingRef " +
            s"returned $undertakingSubsidies",
        errorMessage = s"retrieveSubsidiesForDateRange failed for UndertakingRef:$undertakingRef"
      )

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit eori: EORI, hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector
      .removeSubsidy(undertakingRef, nonHmrcSubsidy)
      .flatMap { response =>
        for {
          ref <- handleResponse[UndertakingRef](response, "remove subsidy").toFuture
          _ <- removedSubsidyRepository.add(eori, nonHmrcSubsidy.copy(removed = Some(true)))
          _ <- undertakingCache.deleteUndertakingSubsidies(ref)
        } yield ref
      }
      .logResult(
        successCall = (undertaking: UndertakingRef) =>
          s"removeSubsidy UndertakingRef:$undertakingRef, NonHmrcSubsidy:$nonHmrcSubsidy returned UndertakingRef:$undertaking",
        errorMessage = s"removeSubsidy failed UndertakingRef:$undertakingRef, NonHmrcSubsidy:$nonHmrcSubsidy"
      )

  def retrieveExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): Future[ExchangeRate] = {
    val cacheKey = YearAndMonth.fromDate(date)

    exchangeRateCache
      .get[ExchangeRate](cacheKey)
      .toContext
      .getOrElseF {
        escConnector
          .retrieveExchangeRate(date)
          .flatMap { response =>
            val result = handleResponse[ExchangeRate](response, "exchange rate")
            exchangeRateCache.put(cacheKey, result)
          }
      }
  }.logResult(
    successCall =
      (exchangeRate: ExchangeRate) => s"retrieveExchangeRate for LocalDate:$date returned ExchangeRate:$exchangeRate",
    errorMessage = s"retrieveExchangeRate failed for LocalDate:$date"
  )

}
