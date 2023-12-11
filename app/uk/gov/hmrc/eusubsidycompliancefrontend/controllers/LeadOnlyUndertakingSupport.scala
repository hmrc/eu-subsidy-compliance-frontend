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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.mvc.Result
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

trait LeadOnlyUndertakingSupport { this: FrontendController =>

  protected val escService: EscService
  protected implicit val executionContext: ExecutionContext

  // Only execute the block where the undertaking exists and the logged in user is the lead for that undertaking, and the undertaking status is not manually suspended.
  // If there is no undertaking, or the user is not the lead we redirect to the account home page.
  // If the undertaking is manually suspended, they are shown the undertaking suspended page. The content changes slightly based on if undertaking is lead or not, hence why the boolean is passed in.
  def withLeadUndertaking[A](
    f: Undertaking => Future[Result]
  )(implicit r: AuthenticatedEnrolledRequest[A], appConfig: AppConfig): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber
    if (appConfig.releaseCEnabled) {
      val retrievedUndertakingOpt = escService.retrieveUndertaking(eori).toContext
      retrievedUndertakingOpt.foldF(Redirect(routes.AccountController.getAccountPage).toFuture) {
        case undertaking if !undertaking.isManuallySuspended && undertaking.isLeadEORI(eori) =>
          f(undertaking)
        case undertaking if !undertaking.isManuallySuspended =>
          Redirect(routes.AccountController.getAccountPage).toFuture
        case undertaking =>
          Redirect(routes.UndertakingSuspendedPageController.showPage(undertaking.isLeadEORI(eori))).toFuture
      }
    } else {
      escService
        .retrieveUndertaking(eori)
        .toContext
        .filter(_.isLeadEORI(r.eoriNumber))
        .foldF(Redirect(routes.AccountController.getAccountPage).toFuture)(f)
    }
  }
}
