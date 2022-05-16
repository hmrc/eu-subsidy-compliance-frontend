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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import play.api.i18n.I18nSupport.RequestWithMessagesApi
import play.api.i18n.MessagesApi
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}

import java.util.Locale
import javax.inject.{Inject, Singleton}
@Singleton
class EmailTemplateHelpers @Inject() (appConfig: AppConfig) {

  private def getLanguage(implicit request: AuthenticatedEscRequest[_], messagesApi: MessagesApi): Language =
    request.request.messages(messagesApi).lang.code.toLowerCase(Locale.UK) match {
      case English.code => English
      case Welsh.code => Welsh
      case other => sys.error(s"Found unsupported language code $other")
    }

  def getEmailTemplateId(inputKey: String)(implicit
    request: AuthenticatedEscRequest[_],
    messagesApi: MessagesApi
  ) = {
    val lang = getLanguage
    appConfig.templateIdsMap(lang.code).getOrElse(inputKey, s"no template for $inputKey")
  }

}
