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

import play.api.Configuration
import play.api.i18n.I18nSupport.RequestWithMessagesApi
import play.api.i18n.MessagesApi
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Language.{English, Welsh}

import java.util.Locale

object EmailTemplateHelpers {

  private def templateIdsMap(config: Configuration, langCode: String) =  Map(
    s"createUndertaking$langCode" -> config.get[String](s"email-send.create-undertaking-template-$langCode"),
    s"addMemberEmailToBE$langCode" -> config.get[String](s"email-send.add-member-to-be-template-$langCode"),
    s"addMemberEmailToLead$langCode" -> config.get[String](s"email-send.add-member-to-lead-template-$langCode"),
    s"removeMemberEmailToBE$langCode" -> config.get[String](s"email-send.remove-member-to-be-template-$langCode"),
    s"removeMemberEmailToLead$langCode" -> config.get[String](s"email-send.remove-member-to-lead-template-$langCode"),
    s"promoteAsLeadEmailToBE$langCode" -> config.get[String](s"email-send.promote-other-as-lead-to-be-template-$langCode"),
    s"promoteAsLeadEmailToLead$langCode" -> config.get[String](s"email-send.promote-other-as-lead-to-lead-template-$langCode")
  )


  private def getLanguage(implicit request: EscAuthRequest[_], messagesApi: MessagesApi): Language =
    request.request.messages(messagesApi).lang.code.toLowerCase(Locale.UK) match {
      case English.code => English
      case Welsh.code   => Welsh
      case other        => sys.error(s"Found unsupported language code $other")
    }

  def getEmailTemplateId(configuration: Configuration, inputKey: String
                   )(implicit request: EscAuthRequest[_], messagesApi: MessagesApi) = {
    val lang = getLanguage
    val mapKey = s"$inputKey${lang.code}"
    templateIdsMap(configuration, lang.code).get(mapKey).getOrElse(s"no template for $mapKey")
  }







}
