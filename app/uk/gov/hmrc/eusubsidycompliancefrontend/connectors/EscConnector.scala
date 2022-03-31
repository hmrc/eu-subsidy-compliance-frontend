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
import play.api.http.Status.OK
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscConnector @Inject() (
  http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  private lazy val escURL: String = servicesConfig.baseUrl("esc")

  private val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  private val updateUndertakingPath = "eu-subsidy-compliance/undertaking/update"
  private val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  private val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  private val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"
  private val updateSubsidyPath = "eu-subsidy-compliance/subsidy/update"
  private val retrieveSubsidyPath = "eu-subsidy-compliance/subsidy/retrieve"

  def createUndertaking(
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[Undertaking, HttpResponse](s"$escURL/$createUndertakingPath", undertaking)
      .map(Right(_))
      .recover {
        case e => Left(ConnectorError(e))
      }

  def updateUndertaking(
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[Undertaking, HttpResponse](s"$escURL/$updateUndertakingPath", undertaking)
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
      }

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    http
      .GET[HttpResponse](s"$escURL/$retrieveUndertakingPath$eori")
      .map { r =>
        if (r.status == OK) Right(r)
        else Left(UpstreamErrorResponse(s"Unexpected response - got HTTP ${r.status}", r.status))
      }

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[BusinessEntity, HttpResponse](s"$escURL/$addMemberPath/$undertakingRef", businessEntity)
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
      }

  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[BusinessEntity, HttpResponse](s"$escURL/$removeMemberPath/$undertakingRef", businessEntity)
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
      }

  def createSubsidy(undertakingRef: UndertakingRef, subsidyUpdate: SubsidyUpdate)(implicit
    hc: HeaderCarrier
  ): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[SubsidyUpdate, HttpResponse](s"$escURL/$updateSubsidyPath", subsidyUpdate)
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
      }

  def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit
    hc: HeaderCarrier
  ): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[SubsidyUpdate, HttpResponse](s"$escURL/$updateSubsidyPath", toSubsidyDelete(nonHmrcSubsidy, undertakingRef))
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
      }

  def retrieveSubsidy(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier): Future[Either[ConnectorError, HttpResponse]] =
    http
      .POST[SubsidyRetrieve, HttpResponse](s"$escURL/$retrieveSubsidyPath", subsidyRetrieve)
      .map(Right(_))
      .recover { case e =>
        Left(ConnectorError(e))
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
