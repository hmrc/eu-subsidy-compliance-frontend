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
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.{ACCEPTED, NOT_FOUND, OK}
import play.api.i18n.I18nSupport.RequestWithMessagesApi
import play.api.i18n.MessagesApi
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.{RetrieveEmailConnector, SendEmailConnector}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIAndDateEmailParameter, DoubleEORIEmailParameter, SingleEORIAndDateEmailParameter, SingleEORIEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.HttpResponseSyntax.HttpResponseOps
import uk.gov.hmrc.http.HeaderCarrier

import java.util.Locale
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (
  appConfig: AppConfig,
  sendEmailConnector: SendEmailConnector,
  retrieveEmailConnector: RetrieveEmailConnector
) extends Logging {

  // TODO - review call sites to determine how missing ref is handled
  def sendEmail(
    eori1: EORI,
    eori2: Option[EORI],
    templateKey: String,
    undertaking: Undertaking,
    undertakingRef: UndertakingRef,
    removeEffectiveDate: Option[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: AuthenticatedEscRequest[_],
    messagesApi: MessagesApi
  ): Future[EmailSendResult] = retrieveEmailByEORI(eori1).flatMap { retrieveEmailResponse =>
    retrieveEmailResponse.emailType match {
      case EmailType.VerifiedEmail =>
        val templateId = getEmailTemplateId(templateKey)
        val emailParameter = buildEmailParameters(templateKey, eori1, eori2, undertaking, undertakingRef, removeEffectiveDate)
        val emailAddress = retrieveEmailResponse.emailAddress.getOrElse(sys.error("email not found"))
        sendEmail(emailAddress, emailParameter, templateId)
      case _ => Future.successful(EmailSendResult.EmailNotSent)
    }
  }

  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveEmailResponse] =
    retrieveEmailConnector.retrieveEmailByEORI(eori).map {
      case Left(error) => throw error
      case Right(value) =>
        value.status match {
          case NOT_FOUND => RetrieveEmailResponse(EmailType.UnVerifiedEmail, None)
          case OK =>
            value
              .parseJSON[EmailAddressResponse]
              .fold(_ => sys.error("Error in parsing Email Address"), handleEmailAddressResponse)

          case _ => sys.error("Error in retrieving Email Address Response")

        }
    }

  private def handleEmailAddressResponse(emailAddressResponse: EmailAddressResponse): RetrieveEmailResponse =
    emailAddressResponse match {
      case EmailAddressResponse(email, Some(_), None) => RetrieveEmailResponse(EmailType.VerifiedEmail, email.some)
      case EmailAddressResponse(email, Some(_), Some(_)) => RetrieveEmailResponse(EmailType.UnDeliverableEmail, email.some)
      case EmailAddressResponse(email, None, None) => RetrieveEmailResponse(EmailType.UnVerifiedEmail, email.some)
      case _ => sys.error("Email address response is not valid")
    }

  private def buildEmailParameters(
    key: String,
    eori1: EORI,
    eori2: Option[EORI],
    undertaking: Undertaking,
    undertakingRef: UndertakingRef,
    removeEffectiveDate: Option[String]
  ): EmailParameters =
    (eori2, removeEffectiveDate) match {
      case (None, None) => SingleEORIEmailParameter(eori1, undertaking.name, undertakingRef, key)
      case (None, Some(date)) => SingleEORIAndDateEmailParameter(eori1, undertaking.name, undertakingRef, date, key)
      case (Some(eori), None) => DoubleEORIEmailParameter(eori1, eori, undertaking.name, undertakingRef, key)
      case (Some(eori), Some(date)) =>
        DoubleEORIAndDateEmailParameter(eori1, eori, undertaking.name, undertakingRef, date, key)
    }

  private def getLanguage(implicit request: AuthenticatedEscRequest[_], messagesApi: MessagesApi): Language =
    request.request.messages(messagesApi).lang.code.toLowerCase(Locale.UK) match {
      case English.code => English
      case Welsh.code => Welsh
      case other => sys.error(s"Unsupported language code: $other")
    }

  private def getEmailTemplateId(inputKey: String)(implicit
    request: AuthenticatedEscRequest[_],
    messagesApi: MessagesApi
  ) = {
    val lang = getLanguage
    appConfig.templateIdsMap(lang.code).getOrElse(inputKey, s"no template for $inputKey")
  }

  private def sendEmail(emailAddress: EmailAddress, emailParameters: EmailParameters, templateId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[EmailSendResult] =
    sendEmailConnector.sendEmail(EmailSendRequest(List(emailAddress), templateId, emailParameters)).map {
      case Left(error) => throw ConnectorError(s"Error in Sending Email ${emailParameters.description}", error)
      case Right(value) =>
        value.status match {
          case ACCEPTED => EmailSendResult.EmailSent
          case other =>
            logger.warn(s"Response for send email call came back with status : $other")
            EmailSendResult.EmailSentFailure
        }
    }

}
