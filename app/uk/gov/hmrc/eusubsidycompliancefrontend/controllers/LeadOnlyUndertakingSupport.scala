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

import play.api.mvc.Result
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

trait LeadOnlyUndertakingSupport { this: FrontendController =>

  protected val escService: EscService
  protected implicit val executionContext: ExecutionContext

  // Only execute the block where the undertaking exists and the logged in user is the lead for that undertaking.
  // If there is no undertaking, or the user is not the lead we redirect to the account home page.
  protected def withLeadUndertaking[A](f: Undertaking => Future[Result])(implicit r: AuthenticatedEscRequest[A]): Future[Result] =
    escService.retrieveUndertaking(r.eoriNumber) flatMap {
      case Some(undertaking) if undertaking.isLeadEORI(r.eoriNumber) => f(undertaking)
      case _ => Redirect(routes.AccountController.getAccountPage()).toFuture
    }

}
