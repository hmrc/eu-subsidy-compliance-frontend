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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions

import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFunction, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

/**
 * ActionBuilder that enforces a lead EORI only restriction.
 *
 * If the logged in user is the lead EORI then the passed in block will be executed, otherwise we redirect to the
 * account home page.
 */
@Singleton
class LeadOnlyActionBuilder @Inject() (escService: EscService)(implicit val executionContext: ExecutionContext)
  extends ActionFunction[EscAuthRequest, EscAuthRequest] with FrontendHeaderCarrierProvider with Logging {

  override def invokeBlock[A](r:  EscAuthRequest[A], f:  EscAuthRequest[A] => Future[Result]): Future[Result] =
    escService.retrieveUndertaking(r.eoriNumber)(hc(r)) flatMap {
      case Some(u) if u.isLeadEORI(r.eoriNumber) => f(r)
      case _ =>
        logger.info("Logged in user is not a lead EORI. Redirecting to account home page.")
        Redirect(routes.AccountController.getAccountPage()).toFuture
    }

}
