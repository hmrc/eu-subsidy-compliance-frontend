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

import cats.implicits.catsSyntaxOptionId
import com.google.inject.Inject
import play.api.http.Status.{ACCEPTED, NOT_FOUND, OK}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{CdsRetrieveEmailConnector, SendEmailConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.{EmailNotSent, EmailSentFailure}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (
  appConfig: AppConfig,
  sendEmailConnector: SendEmailConnector,
  cdsRetrieveEmailConnector: CdsRetrieveEmailConnector,
  emailVerificationService: EmailVerificationService
) extends TracedLogging {

  def findVerifiedEmailPrioritisingCDS(
    eori: EORI
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    val eventualMaybeEmailFromCDS =
      retrieveEmailByEORIFromCds(eori)
        .map(_.emailAddress.map(_.value))

    val eventualEventualMaybeEmailAddress = eventualMaybeEmailFromCDS.flatMap {
      case Some(emailFromCds) =>
        logger.info(s"found email from cds for eori $eori")

        Future.successful(Some(emailFromCds))
      case None =>
        logger.warn(s"$eori could not be found in CDS so deferring to local cache")
        findCachedVerifiedEmail(eori)
    }

    //Trap any errors we have with the EORI. Quite often we have errors but the EORI is unknown so not resolvable
    eventualEventualMaybeEmailAddress.failed.foreach(exception =>
      logger.error(s"failed retrieving email for $eori", exception)
    )

    eventualEventualMaybeEmailAddress
  }

  private def findCachedVerifiedEmail(
    EORI: EORI
  )(implicit ec: ExecutionContext): Future[Option[String]] =
    emailVerificationService
      .getCachedEmailVerification(EORI)
      .map(_.map(_.email))

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
    val parameters: EmailParameters = EmailParameters(
      eori = eori1,
      beEORI = eori2,
      undertakingName = undertaking.name,
      effectiveDate = removeEffectiveDate
    )
    val templateId: String =
      appConfig.getTemplateId(template).getOrElse(s"No template ID for email template: $template")

    findVerifiedEmailPrioritisingCDS(eori1)
      .flatMap { maybeEmail: Option[String] =>
        maybeEmail
          .map { email =>
            sendEmail(EmailAddress(email), parameters, templateId)
          }
          .getOrElse {
            logger.warn(s"No email found for $eori1 so could not send $template")
            Future.successful(EmailNotSent)
          }
      }
      .recover { case e: Throwable =>
        logger.error(s"failed sending email to $eori1 for template $template", e)
        EmailSentFailure
      }
  }

  private def fetchEmailFromCdsAndSend(eori: EORI, parameters: EmailParameters, templateId: String)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[EmailSendResult] =
    retrieveEmailByEORIFromCds(eori).flatMap {
      case RetrieveEmailResponse(EmailType.VerifiedEmail, Some(email)) => sendEmail(email, parameters, templateId)
      case RetrieveEmailResponse(EmailType.VerifiedEmail, None) =>
        Future.failed(sys.error(s"Email address for EORI $eori not found from CDS"))
      case _ => EmailSendResult.EmailNotSent.toFuture
    }

  def retrieveEmailByEORIFromCds(
    eori: EORI
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveEmailResponse] =
    cdsRetrieveEmailConnector.retrieveEmailByEORI(eori).map {
      case Left(error) => throw error
      case Right(value: HttpResponse) =>
        value.status match {
          case NOT_FOUND =>
            RetrieveEmailResponse(emailType = EmailType.UnVerifiedEmail, emailAddress = None)
          case OK =>
            value
              .parseJSON[EmailAddressResponse]
              .fold(_ => sys.error("Error in parsing Email Address"), handleRetrieveEmailAddressResponse)
          case _ =>
            sys.error("Error in retrieving Email Address Response")
        }
    }

  private def handleRetrieveEmailAddressResponse(emailAddressResponse: EmailAddressResponse): RetrieveEmailResponse =
    emailAddressResponse match {
      case EmailAddressResponse(email, Some(_), None) =>
        RetrieveEmailResponse(EmailType.VerifiedEmail, email.some)
      case EmailAddressResponse(email, Some(_), Some(_)) =>
        RetrieveEmailResponse(EmailType.UnDeliverableEmail, email.some)
      case EmailAddressResponse(email, None, None) =>
        RetrieveEmailResponse(EmailType.UnVerifiedEmail, email.some)
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
