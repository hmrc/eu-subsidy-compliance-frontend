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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logging
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.SendEmailConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, Error}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.emailSend.{EmailParameters, EmailSendRequest, EmailSendResult}
import uk.gov.hmrc.http.{HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SendEmailServiceImpl])
trait SendEmailService {
  def sendEmail(emailAddress: EmailAddress, emailParameters: EmailParameters, templateId: String
               )(implicit hc: HeaderCarrier): Future[EmailSendResult]

}

@Singleton
class SendEmailServiceImpl @Inject() (emailSendConnector: SendEmailConnector)(implicit ec: ExecutionContext
) extends SendEmailService
  with Logging {

  override def sendEmail(emailAddress: EmailAddress, emailParameters: EmailParameters, templateId: String
                        )(implicit hc: HeaderCarrier): Future[EmailSendResult] =
    emailSendConnector.sendEmail(EmailSendRequest(List(emailAddress), templateId, emailParameters)).map {
        case Left(Error(_)) => sys.error(s"Error in Sending Email ${emailParameters.description}")
        case Right(value) => value.status match {
          case ACCEPTED => EmailSendResult.EmailSent
          case other    =>
            logger.warn(s"Response for send email call came back with status : $other")
            EmailSendResult.EmailSentFailure
        }
      }




}

