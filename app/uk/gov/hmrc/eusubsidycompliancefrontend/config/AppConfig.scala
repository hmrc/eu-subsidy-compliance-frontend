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
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, contactFrontendConfig: ContactFrontendConfig) {
  val welshLanguageSupportEnabled: Boolean =
    config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  lazy val ggSignInUrl: String = config.get[String](s"urls.ggSignInUrl")
  lazy val ggSignOutUrl: String = config.get[String](s"urls.ggSignOutUrl")
  lazy val eccEscSubscribeUrl: String = config.get[String](s"urls.eccEscSubscribeUrl")
  lazy val exchangeRateToolUrl: String = config.get[String](s"urls.exchangeRateToolUrl")
  lazy val emailFrontendUrl: String = config.get[String]("microservice.services.customs-email-frontend.url")
  lazy val exitSurveyUrl: String = config.get[String]("urls.feedback-survey")
  lazy val contactFrontendUrl: String =
    contactFrontendConfig.baseUrl.getOrElse(sys.error("Could not find config for contact frontend url"))

  lazy val contactFormServiceIdentifier: String =
    contactFrontendConfig.serviceId.getOrElse(sys.error("Could not find config for contact frontend service id"))

  lazy val betaFeedbackUrlNoAuth: String =
    s"$contactFrontendUrl/contact/beta-feedback?service=$contactFormServiceIdentifier"

  lazy val sessionTimeout: String = config.get[String]("application.session.maxAge")

  private lazy val signOutUrlBase: String = config.get[String]("auth.sign-out.url")

  def signOutUrl(continueUrl: Option[String]): String =
    continueUrl.fold(signOutUrlBase)(continue => s"$signOutUrlBase?continue=$continue")

  lazy val timeOutContinue: String = config.get[String](s"urls.timeOutContinue")

  lazy val authTimeoutSeconds: Int = config.get[FiniteDuration]("auth.sign-out.inactivity-timeout").toSeconds.toInt

  lazy val authTimeoutCountdownSeconds: Int =
    config.get[FiniteDuration]("auth.sign-out.inactivity-countdown").toSeconds.toInt


  lazy val templateIds = List(
    // TODO - look into this - the keys here are different to those assumed in the config. Figure out where they
    //        are defined and centralise so they can be used in config and controllers.
    "addMemberToBE",
    "addMemberToLead",
    "createUndertaking",
    "memberRemoveThemselfToBE",
    "memberRemoveThemselfToLead",
    "promoteOtherAsLeadToBE",
    "promoteOtherAsLeadToLead",
    "promotedThemselfToNewLead",
    "removeMemberToBE",
    "removeMemberToLead",
    "removedAsLeadToOldLead",
  ).map(t => t -> config.get[String](s"email-send.$t")).toMap

  // TODO - remove langcode parameter - now unused
  def templateIdsMap(langCode: String): Map[String, String] = templateIds
}
