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

package uk.gov.hmrc.eusubsidycompliancefrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {
  val welshLanguageSupportEnabled: Boolean =
    config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val ggSignInUrl: String = config.get[String](s"urls.ggSignInUrl")
  val ggSignOutUrl: String = config.get[String](s"urls.ggSignOutUrl")
  val eccEscSubscribeUrl: String = config.get[String](s"urls.eccEscSubscribeUrl")
  val exchangeRateToolUrl: String = config.get[String](s"urls.exchangeRateToolUrl")
  val emailFrontendUrl: String = config.get[String]("microservice.services.customs-email-frontend.url")

  val betaFeedbackUrlNoAuth: String = "TODO" // TODO
  lazy val sessionTimeout = config.get[String]("application.session.maxAge")

  def templateIdsMap(config: Configuration, langCode: String) = Map(
    "createUndertaking" -> config.get[String](s"email-send.create-undertaking-template-$langCode"),
    "addMemberEmailToBE" -> config.get[String](s"email-send.add-member-to-be-template-$langCode"),
    "addMemberEmailToLead" -> config.get[String](s"email-send.add-member-to-lead-template-$langCode"),
    "removeMemberEmailToBE" -> config.get[String](s"email-send.remove-member-to-be-template-$langCode"),
    "removeMemberEmailToLead" -> config.get[String](s"email-send.remove-member-to-lead-template-$langCode"),
    "promoteAsLeadEmailToBE" -> config.get[String](s"email-send.promote-other-as-lead-to-be-template-$langCode"),
    "promoteAsLeadEmailToLead" -> config.get[String](s"email-send.promote-other-as-lead-to-lead-template-$langCode"),
    "removeThemselfEmailToBE" -> config.get[String](
      s"email-send.member-remove-themself-email-to-be-template-$langCode"
    ),
    "removeThemselfEmailToLead" -> config.get[String](
      s"email-send.member-remove-themself-email-to-lead-template-$langCode"
    ),
    "promotedAsLeadToNewLead" -> config.get[String](
      s"email-send.promoted-themself-email-to-new-lead-template-$langCode"
    ),
    "removedAsLeadToOldLead" -> config.get[String](s"email-send.removed_as_lead-email-to-old-lead-template-$langCode")
  )
}
