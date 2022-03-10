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
import com.google.inject.{Inject, Singleton}
import play.api.http.Status._
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.RetrieveEmailConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.EmailAddressResponse
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveEmailService @Inject() (retrieveEmailConnector: RetrieveEmailConnector)(implicit ec: ExecutionContext) {
  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier): Future[RetrieveEmailResponse] =
    retrieveEmailConnector.retrieveEmailByEORI(eori).map {
      case Left(error) => throw error
      case Right(value) =>
        value.status match {
          case NOT_FOUND => RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)
          case OK =>
            value
              .parseJSON[EmailAddressResponse]
              .fold(_ => sys.error("Error in parsing Email Address"), getEmailAddress)

          case _ => sys.error("Error in retrieving Email Address Response")

        }
    }

  //If the email is Undeliverable or invalid, it does give a status of OK sometimes but its response is different
  //this method is identifying that response and returning the RetrieveEmailResponse
  private def getEmailAddress(emailAddressResponse: EmailAddressResponse): RetrieveEmailResponse =
    emailAddressResponse match {
      case EmailAddressResponse(email, Some(_), None) => RetrieveEmailResponse(EmailType.VerifiedEmail, email.some)
      case EmailAddressResponse(email, Some(_), Some(_)) =>
        RetrieveEmailResponse(EmailType.UnDeliverableEmail, email.some)
      case EmailAddressResponse(email, None, None) => RetrieveEmailResponse(EmailType.UnVerifiedEmail, email.some)
      case _ => sys.error("Email address response is not valid")
    }

}
