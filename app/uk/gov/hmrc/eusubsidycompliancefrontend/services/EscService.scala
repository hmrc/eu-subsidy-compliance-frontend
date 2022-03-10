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
import com.google.inject.{Inject, Singleton}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Reads
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscService @Inject() (escConnector: EscConnector)(implicit ec: ExecutionContext) {

  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector.createUndertaking(undertaking)
      .map(handleResponse[UndertakingRef](_, "create undertaking"))

  def updateUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    escConnector.updateUndertaking(undertaking)
      .map(handleResponse[UndertakingRef](_, "update undertaking"))

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] =
    escConnector.retrieveUndertaking(eori).map {
      case Left(_) => None
      case Right(value) =>
        value.status match {
          case NOT_FOUND => None
          case OK => value.parseJSON[Undertaking].fold(_ => sys.error("Error in parsing Undertaking"), _.some)
          case _ => sys.error("Error in retrieving Undertaking")
        }
    }

  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.addMember(undertakingRef, businessEntity)
      .map(handleResponse[UndertakingRef](_, "add member"))


  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.removeMember(undertakingRef, businessEntity)
      .map(handleResponse[UndertakingRef](_, "remove member"))

  def createSubsidy(undertakingRef: UndertakingRef, subsidyUpdate: SubsidyUpdate)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.createSubsidy(undertakingRef, subsidyUpdate)
      .map(handleResponse[UndertakingRef](_, "create subsidy"))

  def retrieveSubsidy(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier): Future[UndertakingSubsidies] =
    escConnector.retrieveSubsidy(subsidyRetrieve)
      .map(handleResponse[UndertakingSubsidies](_, "retrieve subsidy"))

  def removeSubsidy(undertakingRef: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(implicit
    hc: HeaderCarrier
  ): Future[UndertakingRef] =
    escConnector.removeSubsidy(undertakingRef, nonHmrcSubsidy)
      .map(handleResponse[UndertakingRef](_, "remove subsidy"))

  private def handleResponse[A](r: Either[Error, HttpResponse], action: String)(implicit reads: Reads[A]) =
    r.fold(_ => sys.error(s"Error executing $action"), { response =>
      if (response.status =!= OK) sys.error(s"Error executing $action - Got response status: ${response.status}")
       else response.parseJSON[A].fold(
          _ => sys.error(s"Error parsing response for $action"),
          identity
        )
    })

}
