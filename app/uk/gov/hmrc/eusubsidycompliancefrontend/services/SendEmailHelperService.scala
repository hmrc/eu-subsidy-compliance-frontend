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

import play.api.Configuration
import play.api.i18n.I18nSupport.RequestWithMessagesApi
import play.api.i18n.MessagesApi
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}
import com.google.inject.Inject
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.{DoubleEORIAndDateEmailParameter, DoubleEORIEmailParameter, SingleEORIAndDateEmailParameter, SingleEORIEmailParameter}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailParameters, EmailSendResult, EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.Locale
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendEmailHelperService @Inject() (
  appConfig: AppConfig,
  retrieveEmailService: RetrieveEmailService,
  sendEmailService: SendEmailService,
  configuration: Configuration
) {

  def retrieveEmailAddressAndSendEmail(
    eori1: EORI,
    eori2: Option[EORI],
    key: String,
    undertaking: Undertaking,
    undertakingRef: UndertakingRef,
    removeEffectiveDate: Option[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: AuthenticatedEscRequest[_],
    messagesApi: MessagesApi
  ): Future[EmailSendResult] =
    for {
      retrieveEmailResponse <- retrieveEmailService.retrieveEmailByEORI(eori1)
      templateId = getEmailTemplateId(configuration, key)
      emailParameter = getEmailParams(key, eori1, eori2, undertaking, undertakingRef, removeEffectiveDate)
      emailAddress = getEmailAddress(retrieveEmailResponse)
      result <- sendEmailService.sendEmail(emailAddress, emailParameter, templateId)
    } yield result

  private def getEmailAddress(retrieveEmailResponse: RetrieveEmailResponse) = retrieveEmailResponse.emailType match {
    case EmailType.VerifiedEmail => retrieveEmailResponse.emailAddress.getOrElse(sys.error("email not found"))
    case _ => sys.error(" No Verified email found")
  }

  private def getEmailParams(
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
      case other => sys.error(s"Found unsupported language code $other")
    }

  private def getEmailTemplateId(configuration: Configuration, inputKey: String)(implicit
    request: AuthenticatedEscRequest[_],
    messagesApi: MessagesApi
  ) = {
    val lang = getLanguage
    appConfig.templateIdsMap(configuration, lang.code).get(inputKey).getOrElse(s"no template for $inputKey")
  }

}
