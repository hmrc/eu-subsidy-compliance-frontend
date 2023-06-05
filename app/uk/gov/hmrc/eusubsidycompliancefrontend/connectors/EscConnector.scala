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
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
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
  private lazy val retrieveExchangeRateUrl = s"$escUrl/eu-subsidy-compliance/exchangerate"

  object emailUrl {
    private val baseEmailUrl = s"$escUrl/eu-subsidy-compliance/email"

    val approveEmailByEori: String = s"$baseEmailUrl/approve/eori"
    val approveEmailByVerificationId: String = s"$baseEmailUrl/approve/verification-id"
    val startVerification: String = s"$baseEmailUrl/start-verification"
    val getVerificationFormat: String = s"$baseEmailUrl/verification-status/%s"
  }

  def createUndertaking(undertaking: UndertakingCreate)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("createUndertaking", createUndertakingUrl, undertaking)

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("updateUndertaking", updateUndertakingUrl, undertaking)

  def disableUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("disableUndertaking", disableUpdateUndertakingUrl, undertaking)

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): EventualConnectorResult = {
    logGet("retrieveUndertaking", s"$retrieveUndertakingUrl/$eori")
  }

  def addMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("addMember", s"$addMemberUrl/$undertakingRef", businessEntity)

  def removeMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("removeMember", s"$removeMemberUrl/$undertakingRef", businessEntity)

  def createSubsidy(subsidyUpdate: SubsidyUpdate)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("createSubsidy", updateSubsidyUrl, subsidyUpdate)

  def removeSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(implicit hc: HeaderCarrier): EventualConnectorResult = {
    val removeSubsidyPayload: SubsidyUpdate = SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy)
    logPost("removeSubsidy", updateSubsidyUrl, removeSubsidyPayload)
  }

  def retrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(implicit hc: HeaderCarrier): EventualConnectorResult = {
    logPost("retrieveSubsidy", retrieveSubsidyUrl, subsidyRetrieve)
  }

  def retrieveExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logGet("retrieveExchangeRate", s"$retrieveExchangeRateUrl/$date")

  def approveEmailByEori(eori: EORI)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("approveEmailByEori", emailUrl.approveEmailByEori, ApproveEmailAsVerifiedByEoriRequest(eori))

  def startVerification(eori: EORI, emailAddress: String)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logPost("startVerification", emailUrl.startVerification, StartEmailVerificationRequest(eori, emailAddress))

  def approveEmailByVerificationId(eori: EORI, verificationId: String)(implicit
    hc: HeaderCarrier
  ): EventualConnectorResult =
    logPost(
      "approveEmailByVerificationId",
      emailUrl.approveEmailByVerificationId,
      ApproveEmailByVerificationIdRequest(eori, verificationId)
    )

  def getEmailVerification(key: EORI)(implicit hc: HeaderCarrier): EventualConnectorResult =
    logGet(
      "getEmailVerification",
      emailUrl.getVerificationFormat.format(key)
    )

}
