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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UpdateEmailAddressController @Inject() (
  mcc: MessagesControllerComponents,
  updateEmailAddressPage: UpdateEmailPage,
  escActionBuilders: EscActionBuilders,
  servicesConfig: ServicesConfig
)(implicit val appConfig: AppConfig)
    extends BaseController(mcc) {
  import escActionBuilders._

  def updateEmailAddress: Action[AnyContent] = escAuthentication.async { implicit request =>
    Future.successful(Ok(updateEmailAddressPage()))
  }

  def postUpdateEmailAddress: Action[AnyContent] = escAuthentication.async { _ =>
    val baseUrl: String = servicesConfig.baseUrl("update-email")
    val updatedEmailUrl: String = s"$baseUrl/manage-email-cds/service/eu-subsidy-compliance-frontend"
    Future.successful(Redirect(updatedEmailUrl))
  }

}
