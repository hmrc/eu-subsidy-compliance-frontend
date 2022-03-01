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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, NonHmrcSubsidy, SubsidyRetrieve, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EscServiceImpl])
trait EscService {
  def createUndertaking(
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier): Future[UndertakingRef]
  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef]
  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]]
  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef]
  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef]
  def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef]
  def retrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(implicit hc: HeaderCarrier): Future[UndertakingSubsidies]
  def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef]
}

@Singleton
class EscServiceImpl @Inject() (escConnector: EscConnector)(implicit
  ec: ExecutionContext
) extends EscService {

  override def createUndertaking(
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector.createUndertaking(undertaking).map {
      case Left(Error(_)) =>
        sys.error("Error in creating Undertaking")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in creating Undertaking")
        else
          value.parseJSON[UndertakingRef].fold(_ => sys.error("Error in parsing  UndertakingRef"), identity)
    }

  override def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector.updateUndertaking(undertaking).map {
      case Left(Error(_)) => sys.error(" Error in updating undertaking")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in Update undertaking as http response came back with non OK status")
        else value.parseJSON[UndertakingRef].fold(_ => sys.error("Error in parsing  UndertakingRef"), identity)
    }

  override def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] =
    escConnector.retrieveUndertaking(eori).map {
      case Left(Error(_)) => None
      case Right(value) =>
        value.status match {
          case NOT_FOUND => None
          case OK => value.parseJSON[Undertaking].fold(_ => sys.error("Error in parsing Undertaking"), _.some)
          case _ => sys.error("Error in retrieving Undertaking")
        }
    }

  override def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.addMember(undertakingRef, businessEntity).map {
      case Left(Error(_)) => sys.error("Error in adding member to the Business Entity")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in adding member to the Business Entity")
        else
          value.parseJSON[UndertakingRef].fold(_ => sys.error("Error in parsing  Undertaking Ref"), identity)
    }

  override def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.removeMember(undertakingRef, businessEntity).map {
      case Left(Error(_)) => sys.error("Error in removing member from the Business Entity")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in removing member from the Business Entity")
        else
          value.parseJSON[UndertakingRef].fold(_ => sys.error("Error in parsing  Undertaking Ref"), identity)
    }

  override def createSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.createSubsidy(undertakingRef, journey).map {
      case Left(Error(_)) => sys.error("Error in creating subsidy")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in creating subsidy ")
        else
          value.parseJSON[UndertakingRef].fold(_ => sys.error("Error in parsing  Undertaking Ref"), identity)
    }

  override def retrieveSubsidy(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier): Future[UndertakingSubsidies] =
    escConnector.retrieveSubsidy(subsidyRetrieve).map {
      case Left(Error(_)) => sys.error("Error in retrieving subsidy")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in retrieving subsidy ")
        else
          value
            .parseJSON[UndertakingSubsidies]
            .fold(_ => sys.error("Error in parsing  UndertakingSubsidies "), identity)
    }

  override def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.removeSubsidy(undertakingRef, nonHmrcSubsidy).map {
      case Left(Error(_)) => sys.error("Error in removing subsidy")
      case Right(value) =>
        if (value.status =!= OK) sys.error("Error in removing subsidy ")
        else
          value
            .parseJSON[UndertakingRef]
            .fold(
              _ => sys.error("Error in parsing  UndertakingSubsidies "),
              undertakingSubsidies => undertakingSubsidies
            )
    }
}
