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


import play.api.Logger
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidyAmendment}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EscConnectorImpl])
trait EscConnector {
  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def retrieveSubsidy(subsidyRetrieve : SubsidyRetrieve)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]

}

@Singleton
class EscConnectorImpl @Inject()(http: HttpClient,
                                 servicesConfig: ServicesConfig,
                                 timeProvider: TimeProvider
                                )(implicit ec: ExecutionContext
) extends EscConnector {

  val logger: Logger = Logger(this.getClass)

  val escURL: String = servicesConfig.baseUrl("esc")
  val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"
  val updateSubsidyPath = "eu-subsidy-compliance/subsidy/update"
  val retrieveSubsidyPath = "eu-subsidy-compliance/subsidy/retrieve"


  override def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    val createUndertakingUrl = s"$escURL/$createUndertakingPath"
    http.
      POST[Undertaking, HttpResponse](createUndertakingUrl, undertaking)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  override def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    {
      val retrieveUndertakingUrl = s"$escURL/$retrieveUndertakingPath$eori"
      http.
        GET[HttpResponse](retrieveUndertakingUrl)
        .map(Right(_))
        .recover {
          case _: NotFoundException => Right(HttpResponse(NOT_FOUND, ""))
          case ex => Left(Error(ex))
        }
    }
  }

  override def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
   val addMemberUrl = s"$escURL/$addMemberPath/$undertakingRef"
    http
      .POST[BusinessEntity, HttpResponse](addMemberUrl, businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  override def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    val removeMemberUrl = s"$escURL/$removeMemberPath/$undertakingRef"
    http
      .POST[BusinessEntity, HttpResponse](removeMemberUrl, businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(
    implicit hc: HeaderCarrier
  ): Future[Either[Error, HttpResponse]] = {
   val subsidyUpdate = toSubsidyUpdate(journey, undertakingRef)
    val createSubsidyUrl = s"$escURL/$updateSubsidyPath"
    http
      .POST[SubsidyUpdate, HttpResponse](createSubsidyUrl, subsidyUpdate)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  override def retrieveSubsidy(subsidyRetrieve : SubsidyRetrieve)(
    implicit hc: HeaderCarrier
  ): Future[Either[Error, HttpResponse]] = {
    val retrieveSubsidyPathUrl = s"$escURL/$retrieveSubsidyPath"
    http
      .POST[SubsidyRetrieve, HttpResponse](retrieveSubsidyPathUrl, subsidyRetrieve)
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
            subsidyUsageTransactionId = None,
            allocationDate = currentDate,
            submissionDate= currentDate,
            publicAuthority = Some(journey.publicAuthority.value.getOrElse(sys.error(" publicAuthority is missing"))),// this shouldn't be optional, is required in create API but not retrieve
            traderReference = journey.traderRef.value.getOrElse(sys.error(" trader ref is missing")),
            nonHMRCSubsidyAmtEUR = SubsidyAmount(journey.claimAmount.value.getOrElse(sys.error(" claimAmount is missing"))),
            businessEntityIdentifier = journey.addClaimEori.value.getOrElse(sys.error(" addClaimEori is missing")),
            amendmentType = Some(EisSubsidyAmendmentType("1"))
          )
        )
      )
    )
  }

}

