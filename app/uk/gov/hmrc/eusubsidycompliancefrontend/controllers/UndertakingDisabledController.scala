/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.UndertakingDisabledPage
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UndertakingDisabledController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  undertakingDisabledPage: UndertakingDisabledPage
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {
  import actionBuilders._

  def getUndertakingDisabled: Action[AnyContent] =
    verifiedEori.async { implicit request =>
      Ok(undertakingDisabledPage()).withNewSession.toFuture
    }
}
