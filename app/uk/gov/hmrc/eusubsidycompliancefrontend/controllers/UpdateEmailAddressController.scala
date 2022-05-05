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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UpdateEmailAddressController @Inject() (
  mcc: MessagesControllerComponents,
  updateUnverifiedEmailAddressPage: UpdateUnverifiedEmailPage,
  updateUndeliveredEmailAddressPage: UpdateUndeliveredEmailAddressPage,
  escCDSActionBuilder: EscCDSActionBuilders
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc) {
  import escCDSActionBuilder._

  def updateUnverifiedEmailAddress: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    Ok(updateUnverifiedEmailAddressPage()).toFuture
  }

  def postUpdateEmailAddress: Action[AnyContent] = withCDSAuthenticatedUser.async {
    Redirect(appConfig.emailFrontendUrl).toFuture
  }

  def updateUndeliveredEmailAddress: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    Ok(updateUndeliveredEmailAddressPage()).toFuture
  }

}
