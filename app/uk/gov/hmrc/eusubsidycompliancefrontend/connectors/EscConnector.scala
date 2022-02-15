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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidyAmendment}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EscConnectorImpl])
trait EscConnector {
  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def retrieveSubsidy(subsidyRetrieve : SubsidyRetrieve)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]

}

@Singleton
class EscConnectorImpl @Inject()(
  http: HttpClient,
  servicesConfig: ServicesConfig,
  timeProvider: TimeProvider
)(implicit ec: ExecutionContext) extends EscConnector {

  private val escURL: String = servicesConfig.baseUrl("esc")

  private val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  private val updateUndertakingPath = "eu-subsidy-compliance/undertaking/update"
  private val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  private val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  private val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"
  private val updateSubsidyPath = "eu-subsidy-compliance/subsidy/update"
  private val retrieveSubsidyPath = "eu-subsidy-compliance/subsidy/retrieve"


  override def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http
      .POST[Undertaking, HttpResponse](s"$escURL/$createUndertakingPath", undertaking)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http.
      POST[Undertaking, HttpResponse](s"$escURL/$updateUndertakingPath", undertaking)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http.
      GET[HttpResponse](s"$escURL/$retrieveUndertakingPath$eori")
      .map(Right(_))
      .recover {
        case _: NotFoundException => Right(HttpResponse(NOT_FOUND, ""))
        case ex => Left(Error(ex))
      }

  override def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
   http
      .POST[BusinessEntity, HttpResponse](s"$escURL/$addMemberPath/$undertakingRef", businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http
      .POST[BusinessEntity, HttpResponse](s"$escURL/$removeMemberPath/$undertakingRef", businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http
      .POST[SubsidyUpdate, HttpResponse](s"$escURL/$updateSubsidyPath", toSubsidyUpdate(journey, undertakingRef))
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http
      .POST[SubsidyUpdate, HttpResponse](s"$escURL/$updateSubsidyPath", toSubsidyDelete(nonHmrcSubsidy, undertakingRef))
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }

  override def retrieveSubsidy(subsidyRetrieve : SubsidyRetrieve)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    println(s"retrieveSubsidy: $subsidyRetrieve")
    http
      .POST[SubsidyRetrieve, HttpResponse](s"$escURL/$retrieveSubsidyPath", subsidyRetrieve)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  private def toSubsidyUpdate(journey: SubsidyJourney, undertakingRef: UndertakingRef) = {
    val currentDate = timeProvider.today
    SubsidyUpdate(
      undertakingIdentifier = undertakingRef,
      update = UndertakingSubsidyAmendment(
        List(
          NonHmrcSubsidy(
            subsidyUsageTransactionID = journey.existingTransactionId,
            allocationDate = journey.claimDate.value.get.toLocalDate,
            submissionDate = currentDate,
            publicAuthority = Some(journey.publicAuthority.value.get),// this shouldn't be optional, is required in create API but not retrieve
            traderReference = journey.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
            nonHMRCSubsidyAmtEUR = SubsidyAmount(journey.claimAmount.value.get),
            businessEntityIdentifier = journey.addClaimEori.value.fold(sys.error("eori value missing"))(oprionalEORI => oprionalEORI.value.map(EORI(_))),
            amendmentType = journey.existingTransactionId.fold(Some(EisSubsidyAmendmentType("1")))(_ => Some(EisSubsidyAmendmentType("2")))
          )
        )
      )
    )
  }

  private def toSubsidyDelete(nonHmrcSubsidy: NonHmrcSubsidy, undertakingRef: UndertakingRef) =
    SubsidyUpdate(
      undertakingIdentifier = undertakingRef,
      update = UndertakingSubsidyAmendment(
        List(
          nonHmrcSubsidy.copy(amendmentType = Some(EisSubsidyAmendmentType("3")))
        )
      )
    )

}

