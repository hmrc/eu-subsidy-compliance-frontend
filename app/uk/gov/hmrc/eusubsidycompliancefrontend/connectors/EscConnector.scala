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
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.ExecutionContext

@Singleton
class EscConnector @Inject() (
  override protected val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends Connector
    with Logging {

  private lazy val escUrl: String = servicesConfig.baseUrl("esc")

  //euSubsidyComplianceFrontend is camelCased as kibana breaks words on hyphens
  logger.error(s"escUrl for euSubsidyComplianceFrontend is $escUrl")

  private lazy val createUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val updateUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking/update"
  private lazy val disableUpdateUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking/disable"
  private lazy val retrieveUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val addMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member"
  private lazy val removeMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member/remove"
  private lazy val updateSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/update"
  private lazy val retrieveSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/retrieve"
  private lazy val retrieveExchangeRateUrl = s"$escUrl/eu-subsidy-compliance/exchangerate"

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

  def retrieveExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): ConnectorResult =
    logGet("retrieveExchangeRate", s"$retrieveExchangeRateUrl/$date")

}
