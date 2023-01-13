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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders

import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.RequestSyntax.RequestOps
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

trait EscActionBuilder extends AuthRedirects {

  def handleFailure(request: Request[_], appConfig: AppConfig): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(appConfig.ggSignInUrl, Map("continue" -> Seq(request.toRedirectTarget), "origin" -> Seq(origin)))
  }

}

object EscActionBuilder {
  val EccEnrolmentKey = "HMRC-ESC-ORG"
  val EccEnrolmentIdentifier = "EORINumber"
}