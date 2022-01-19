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

import cats.implicits.catsSyntaxOptionId
import com.google.inject.{ImplementedBy, Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.HttpResponseOps.HttpResponseOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EscServiceImpl])
trait EscService {
  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef]
  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]]
  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[UndertakingRef]
  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    implicit hc: HeaderCarrier
  ): Future[UndertakingRef]

}

@Singleton
class EscServiceImpl @Inject() (
                                 escConnector: EscConnector
                               )(implicit ec: ExecutionContext)
  extends EscService {

  override def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] = {
    escConnector.createUndertaking(undertaking).map {
      case Left(Error(_)) =>
        sys.error("Error in creating Undertaking")
      case Right(value) =>
        value.parseJSON[UndertakingRef].fold(_ =>  sys.error("Error in parsing  UndertakingRef"), undertakingRef => undertakingRef)
    }
  }

  override def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] = {
    escConnector.retrieveUndertaking(eori).map {
      case Left(Error(_)) => None
      case Right(value) => value.parseJSON[Undertaking].fold(_ => None, _.some)
    }
  }

  override def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[UndertakingRef] = {
    escConnector.addMember(undertakingRef, businessEntity).map {
      case Left(Error(_)) =>  sys.error("Error in adding member to the Business Entity")
      case Right(value) => value.parseJSON[UndertakingRef].fold(_ =>  sys.error("Error in parsing  Undertaking Ref"),undertakingRef => undertakingRef)
    }
  }

  override def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[UndertakingRef] = {
    escConnector.removeMember(undertakingRef, businessEntity).map {
      case Left(Error(_)) =>  sys.error("Error in removing member from the Business Entity")
      case Right(value) => value.parseJSON[UndertakingRef].fold(_ =>  sys.error("Error in parsing  Undertaking Ref"),undertakingRef => undertakingRef)
    }
  }
}

