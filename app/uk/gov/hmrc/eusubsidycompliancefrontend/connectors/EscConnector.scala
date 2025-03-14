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

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.LocalDate

@Singleton
class EscConnector @Inject() (
  override protected val http: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends Connector
    with Logging {

  private lazy val escUrl: String = servicesConfig.baseUrl("esc")

  private lazy val createUndertakingUrl = url"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val updateUndertakingUrl = url"$escUrl/eu-subsidy-compliance/undertaking/update"
  private lazy val disableUpdateUndertakingUrl = url"$escUrl/eu-subsidy-compliance/undertaking/disable"
  private lazy val retrieveUndertakingUrl = url"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val addMemberUrl = url"$escUrl/eu-subsidy-compliance/undertaking/member"
  private lazy val removeMemberUrl = url"$escUrl/eu-subsidy-compliance/undertaking/member/remove"
  private lazy val updateSubsidyUrl = url"$escUrl/eu-subsidy-compliance/subsidy/update"
  private lazy val retrieveSubsidyUrl = url"$escUrl/eu-subsidy-compliance/subsidy/retrieve"
  private lazy val getUndertakingBalanceUrl = url"$escUrl/eu-subsidy-compliance/undertaking/balance"
  private lazy val getExchangeRateUrl = url"$escUrl/eu-subsidy-compliance/retrieve-exchange-rate"

  def createUndertaking(undertaking: UndertakingCreate)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(
      _.post(createUndertakingUrl)
        .withBody(Json.toJson(undertaking))
        .execute[HttpResponse]
    )

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.POST(updateUndertakingUrl, undertaking))
    makeRequest(
      _.post(updateUndertakingUrl)
        .withBody(Json.toJson(undertaking))
        .execute[HttpResponse]
    )

  def disableUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.POST(disableUpdateUndertakingUrl, undertaking))
    makeRequest(
      _.post(disableUpdateUndertakingUrl)
        .withBody(Json.toJson(undertaking))
        .execute[HttpResponse]
    )

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.GET(s"$retrieveUndertakingUrl/$eori"))
    makeRequest(
      _.get(url"$retrieveUndertakingUrl/$eori")
        .execute[HttpResponse]
    )

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): ConnectorResult =
    //makeRequest(_.POST(s"$addMemberUrl/$undertakingRef", businessEntity))
    makeRequest(
      _.post(url"$addMemberUrl/$undertakingRef")
        .withBody(Json.toJson(businessEntity))
        .execute[HttpResponse]
    )

  def removeMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.POST(s"$removeMemberUrl/$undertakingRef", businessEntity))
    makeRequest(
      _.post(url"$removeMemberUrl/$undertakingRef")
        .withBody(Json.toJson(businessEntity))
        .execute[HttpResponse]
    )

  def createSubsidy(subsidyUpdate: SubsidyUpdate)(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.POST(updateSubsidyUrl, subsidyUpdate))
    makeRequest(
      _.post(updateSubsidyUrl)
        .withBody(Json.toJson(subsidyUpdate))
        .execute[HttpResponse]
    )

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit hc: HeaderCarrier): ConnectorResult = {
    val removeSubsidyPayload = SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy)

    makeRequest(
      _.post(updateSubsidyUrl)
        .withBody(Json.toJson(removeSubsidyPayload))
        .execute[HttpResponse]
    )
  }

  def retrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(implicit hc: HeaderCarrier): ConnectorResult =
    //makeRequest(_.POST(retrieveSubsidyUrl, subsidyRetrieve))
    makeRequest(
      _.post(retrieveSubsidyUrl)
        .withBody(Json.toJson(subsidyRetrieve))
        .execute[HttpResponse]
    )

  def getUndertakingBalance(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[UndertakingBalance]] = {
    http
      .get(url"$getUndertakingBalanceUrl/$eori")
      .execute[HttpResponse]
      .map { response: HttpResponse =>
        response.status match {
          case Status.OK => response.json.asOpt[UndertakingBalance]
          case _ => None
        }
      }
      .recover { case e: NotFoundException =>
        logger.warn(s"undertaking balance for eori: $eori not found. Exception: $e", e)
        None
      }
  }

  def getExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): Future[Option[MonthlyExchangeRate]] = {
    val dateAsString = date.toString
    http
      .get(url"$getExchangeRateUrl/$dateAsString")
      .execute[HttpResponse]
      .map { response: HttpResponse =>
        response.status match {
          case Status.OK => response.json.asOpt[MonthlyExchangeRate]
          case _ => None
        }
      }
      .recover { case e: NotFoundException =>
        logger.warn(s"Unable to retrieve exchange rate because of exception: $e", e)
        None
      }
  }
}
