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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidies, UndertakingSubsidyAmendment}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscConnector @Inject()(
  val http: HttpClient,
  val servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) extends DesHelpers {

  val logger: Logger = Logger(this.getClass)
  val escURL: String = servicesConfig.baseUrl("esc")
  val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"

  val updateSubsidyPath = "eu-subsidy-compliance/subsidy/update"
  val retrieveSubsidyPath = "eu-subsidy-compliance/subsidy/retrieve"

  def retrieveUndertaking(
                           eori: EORI
                         )(
                           implicit hc: HeaderCarrier
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
                         implicit hc: HeaderCarrier
                       ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[Undertaking, UndertakingRef](
      s"$escURL/$createUndertakingPath",
      undertaking
    )
  }

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    implicit hc: HeaderCarrier
  ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[BusinessEntity, UndertakingRef](
      s"$escURL/$addMemberPath/$undertakingRef",
      businessEntity
    )
  }

  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    implicit hc: HeaderCarrier
  ): Future[UndertakingRef] = {
    implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
    desPost[BusinessEntity, UndertakingRef](
      s"$escURL/$removeMemberPath/$undertakingRef",
      businessEntity
    )
  }

  def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UndertakingRef] = {
   val update = SubsidyUpdate(
     undertakingIdentifier = undertakingRef,
     update = UndertakingSubsidyAmendment(
       List(
         NonHmrcSubsidy(
           subsidyUsageTransactionId = None,
           allocationDate = LocalDate.now(),
           submissionDate= LocalDate.now(),
           publicAuthority = Some(journey.publicAuthority.value.get),// this shouldn't be optional, is required in create API but not retrieve
           traderReference = journey.traderRef.value.get,
           nonHMRCSubsidyAmtEUR = SubsidyAmount(journey.claimAmount.value.get),
           businessEntityIdentifier = journey.addClaimEori.value.get,
           amendmentType = Some(EisSubsidyAmendmentType("1"))
         )
       )
     )
   )
  desPost[SubsidyUpdate, UndertakingRef](
    s"$escURL/$updateSubsidyPath",
    update
  )

  }

  def retrieveSubsidy(subsidyRetrieve : SubsidyRetrieve)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UndertakingSubsidies] = {
    implicit val undertakingSubsidiesFormat: OFormat[UndertakingSubsidies] = Json.format[UndertakingSubsidies]
    desPost[SubsidyRetrieve, UndertakingSubsidies](
      s"$escURL/$retrieveSubsidyPath",
      subsidyRetrieve
    )
  }
}
