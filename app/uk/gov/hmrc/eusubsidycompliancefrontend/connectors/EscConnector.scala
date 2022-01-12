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

import cats.implicits._

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OFormat}
import play.api.{Logger, Mode}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscConnector @Inject()(
  val http: HttpClient,
  val mode: Mode,
  val servicesConfig: ServicesConfig,
  val auditing: AuditConnector,
  ec: ExecutionContext
) extends DesHelpers {

  val logger: Logger = Logger(this.getClass)
  val escURL: String = servicesConfig.baseUrl("esc")
  val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"

  def retrieveUndertaking(
                           eori: EORI
                         )(
                           implicit hc: HeaderCarrier,
                           ec: ExecutionContext
                         ): Future[Option[Undertaking]] = {

    import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.undertakingFormat
    import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
    desGet[Either[UpstreamErrorResponse, Undertaking]](
      s"$escURL/$retrieveUndertakingPath$eori"
    ).map {
      case Left(UpstreamErrorResponse(_, 404, _, _)) =>
        Option.empty[Undertaking]
      case Right(value) =>
        value.some
    }
  }

  def createUndertaking(
                         undertaking: Undertaking
                       )(
                         implicit hc: HeaderCarrier,
                         ec: ExecutionContext
                       ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[Undertaking, UndertakingRef](
      s"$escURL/$createUndertakingPath",
      undertaking
    )
  }

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[BusinessEntity, UndertakingRef](
      s"$escURL/$addMemberPath/$undertakingRef",
      businessEntity
    )
  }

  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[BusinessEntity, UndertakingRef](
      s"$escURL/$removeMemberPath/$undertakingRef",
      businessEntity
    )
  }
}
