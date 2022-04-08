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

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

@Singleton
class EscConnector @Inject() (
  override protected val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) extends Connector {

  private lazy val escUrl: String = servicesConfig.baseUrl("esc")

  private lazy val createUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val updateUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking/update"
  private lazy val retrieveUndertakingUrl = s"$escUrl/eu-subsidy-compliance/undertaking"
  private lazy val addMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member"
  private lazy val removeMemberUrl = s"$escUrl/eu-subsidy-compliance/undertaking/member/remove"
  private lazy val updateSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/update"
  private lazy val retrieveSubsidyUrl = s"$escUrl/eu-subsidy-compliance/subsidy/retrieve"

  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[Undertaking, HttpResponse](createUndertakingUrl, undertaking))

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[Undertaking, HttpResponse](updateUndertakingUrl, undertaking))

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.GET[HttpResponse](s"$retrieveUndertakingUrl/$eori"))

  def addMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[BusinessEntity, HttpResponse](s"$addMemberUrl/$undertakingRef", businessEntity))

  def removeMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[BusinessEntity, HttpResponse](s"$removeMemberUrl/$undertakingRef", businessEntity))

  def createSubsidy(subsidyUpdate: SubsidyUpdate)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[SubsidyUpdate, HttpResponse](updateSubsidyUrl, subsidyUpdate))

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[SubsidyUpdate, HttpResponse](
      updateSubsidyUrl, SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy)
    ))

  def retrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(implicit hc: HeaderCarrier): ConnectorResult =
    makeRequest(_.POST[SubsidyRetrieve, HttpResponse](retrieveSubsidyUrl, subsidyRetrieve))

}
