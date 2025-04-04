/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.{ACCEPTED, NOT_FOUND, OK}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{CustomsDataStoreConnector, SendEmailConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.VerifiedStatus.{Verified, VerifiedStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.VerifiedEoriCache
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (
  appConfig: AppConfig,
  sendEmailConnector: SendEmailConnector,
  customsDataStoreConnector: CustomsDataStoreConnector,
  verifiedEoriCache: VerifiedEoriCache
) extends Logging {

  def sendEmail(
    eori1: EORI,
    template: EmailTemplate,
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailSendResult] =
    sendEmail(eori1, None, template, undertaking, None)

  def sendEmail(
    eori1: EORI,
    eori2: EORI,
    template: EmailTemplate,
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailSendResult] =
    sendEmail(eori1, eori2.some, template, undertaking, None)

  def sendEmail(
    eori1: EORI,
    template: EmailTemplate,
    undertaking: Undertaking,
    removeEffectiveDate: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailSendResult] =
    sendEmail(eori1, None, template, undertaking, removeEffectiveDate.some)

  def sendEmail(
    eori1: EORI,
    eori2: EORI,
    template: EmailTemplate,
    undertaking: Undertaking,
    removeEffectiveDate: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailSendResult] =
    sendEmail(eori1, eori2.some, template, undertaking, removeEffectiveDate.some)

  private def sendEmail(
    eori1: EORI,
    eori2: Option[EORI],
    template: EmailTemplate,
    undertaking: Undertaking,
    removeEffectiveDate: Option[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[EmailSendResult] = {

    val parameters = EmailParameters(eori1, eori2, undertaking.name, removeEffectiveDate)
    val templateId = appConfig.getTemplateId(template).getOrElse(s"No template ID for email template: $template")

    retrieveEmailByEORI(eori1).flatMap {
      case RetrieveEmailResponse(EmailType.VerifiedEmail, Some(email)) => sendEmail(email, parameters, templateId)
      case RetrieveEmailResponse(EmailType.VerifiedEmail, None) => Future.failed(sys.error("Email address not found"))
      case _ => EmailSendResult.EmailNotSent.toFuture
    }
  }

  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveEmailResponse] =
    customsDataStoreConnector.retrieveEmailByEORI(eori).map { value =>
      value.status match {
        case NOT_FOUND => RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)
        case OK =>
          value
            .parseJSON[EmailAddressResponse]
            .fold(_ => sys.error("Error in parsing Email Address"), handleEmailAddressResponse)
        case _ => sys.error("Error in retrieving Email Address Response")
      }
    }

  def hasVerifiedEmail(eori: EORI)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[VerifiedStatus]] =
    verifiedEoriCache.get(eori).flatMap {
      case Some(_) => Future.successful(Some(Verified))
      case None =>
        for {
          resp <- retrieveEmailByEORI(eori).flatMap {
            case RetrieveEmailResponse(EmailType.VerifiedEmail, _) =>
              verifiedEoriCache.put(VerifiedEori(eori)).as(Some(Verified))
            case _ => Future.successful(None)
          }
        } yield resp
    }

  def retrieveVerifiedEmailAddressByEORI(eori: EORI)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    retrieveEmailByEORI(eori).map { res =>
      res.emailAddress match {
        case Some(email) => email.value
        case None => sys.error(s"Error in retrieving email address for eori: $eori")
      }
    }

  def updateEmailForEori(eori: EORI, emailAddress: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    customsDataStoreConnector.updateEmailForEori(eori, emailAddress)

  private def handleEmailAddressResponse(emailAddressResponse: EmailAddressResponse): RetrieveEmailResponse =
    emailAddressResponse match {
      case EmailAddressResponse(email, Some(_), None) => RetrieveEmailResponse(EmailType.VerifiedEmail, email.some)
      case EmailAddressResponse(email, Some(_), Some(_)) =>
        RetrieveEmailResponse(EmailType.UnDeliverableEmail, email.some)
      case EmailAddressResponse(email, None, None) => RetrieveEmailResponse(EmailType.UnVerifiedEmail, email.some)
      case _ => sys.error("Email address response is not valid")
    }

  private def sendEmail(emailAddress: EmailAddress, parameters: EmailParameters, templateId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[EmailSendResult] =
    sendEmailConnector.sendEmail(EmailSendRequest(List(emailAddress), templateId, parameters)).map {
      case Left(error) => throw ConnectorError(s"Error sending email: $templateId", error)
      case Right(value) =>
        value.status match {
          case ACCEPTED => EmailSendResult.EmailSent
          case other =>
            logger.warn(s"Response for send email call came back with status : $other")
            EmailSendResult.EmailSentFailure
        }
    }

}
