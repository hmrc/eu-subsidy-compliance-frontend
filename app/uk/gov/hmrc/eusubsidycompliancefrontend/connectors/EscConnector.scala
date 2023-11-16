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
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscConnector @Inject() (
  override protected val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends Connector
    with TracedLogging {

  private lazy val escUrl: String = servicesConfig.baseUrl("esc")

  private lazy val createUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val updateUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking/update"
  private lazy val disableUpdateUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking/disable"
  private lazy val retrieveUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val addMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member"
  private lazy val removeMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member/remove"
  private lazy val updateSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/update"
  private lazy val retrieveSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/retrieve"
  private lazy val getUndertakingBalanceUrl = s"$escUrl/eu-subsidy-compliance/undertaking/balance"

  def createUndertaking(undertaking: UndertakingCreate)(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("createUndertaking", createUndertakingUrl, undertaking)

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("updateUndertaking", updateUndertakingUrl, undertaking)

  def disableUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("disableUndertaking", disableUpdateUndertakingUrl, undertaking)

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): ConnectorResult = {
    logGet("retrieveUndertaking", s"$retrieveUndertakingUrl/$eori")
  }

  def addMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("addMember", s"$addMemberUrl/$undertakingRef", businessEntity)

  def removeMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("removeMember", s"$removeMemberUrl/$undertakingRef", businessEntity)

  def createSubsidy(subsidyUpdate: SubsidyUpdate)(implicit hc: HeaderCarrier): ConnectorResult =
    logPost("createSubsidy", updateSubsidyUrl, subsidyUpdate)

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit hc: HeaderCarrier): ConnectorResult = {
    val removeSubsidyPayload = SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy)
    logPost("removeSubsidy", updateSubsidyUrl, removeSubsidyPayload)
  }

  def retrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(implicit hc: HeaderCarrier): ConnectorResult = {
    logPost("retrieveSubsidy", retrieveSubsidyUrl, subsidyRetrieve)
  }

  def getUndertakingBalance(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[UndertakingBalance]] = {
    http
      .GET(s"$getUndertakingBalanceUrl/$eori")
      .map { response: HttpResponse =>
        response.status match {
          case Status.OK => Json.parse(response.body).asOpt[UndertakingBalance]
          case _ => None
        }
      }
      .recover {
        case e: NotFoundException => {
          logger.warn(s"undertaking balance for eori: $eori not found. Exception: $e")
          None
        }
      }
  }
}
