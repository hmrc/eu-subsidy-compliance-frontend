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
import play.api.http.Status._
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.RetrieveEmailConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.util.HttpResponseOps.HttpResponseOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RetrieveEmailServiceImpl])
trait RetrieveEmailService {

  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[EmailAddress]]

}

@Singleton
class RetrieveEmailServiceImpl @Inject() (retrieveEmailConnector: RetrieveEmailConnector
                                         )(implicit ec: ExecutionContext) extends RetrieveEmailService {
  override def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[EmailAddress]] = {
    retrieveEmailConnector.retrieveEmailByEORI(eori).map {
      case Left(Error(_)) => sys.error("Error in retrieving Email Address")
      case Right(value) =>
        value.status match {
          case NOT_FOUND => None
          case OK => value.parseJSON[EmailAddress].fold(_ => sys.error("Error in parsing Email Address"), _.some)
          case _ => sys.error("Error in retrieving Email Address")
        }
    }
  }
}
