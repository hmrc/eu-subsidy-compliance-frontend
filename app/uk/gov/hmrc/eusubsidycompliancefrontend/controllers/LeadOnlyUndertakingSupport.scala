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

import cats.data.OptionT
import play.api.mvc.Result
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

trait LeadOnlyUndertakingSupport { this: FrontendController =>

  protected val escService: EscService
  protected val store: Store
  protected implicit val executionContext: ExecutionContext

  // Only execute the block where the undertaking exists and the logged in user is the lead for that undertaking.
  // If there is no undertaking, or the user is not the lead we redirect to the account home page.
  def withLeadUndertaking[A](f: Undertaking => Future[Result])(implicit r: AuthenticatedEscRequest[A]): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber

    store.get[Undertaking].toContext
      .orElse(getAndCacheUndertaking)
      .filter(_.isLeadEORI(r.eoriNumber))
      .foldF(Redirect(routes.AccountController.getAccountPage()).toFuture)(f)
  }

  private def getAndCacheUndertaking[A](implicit r: AuthenticatedEscRequest[A], e: EORI): OptionT[Future, Undertaking] =
    for {
      undertaking <- escService.retrieveUndertaking(e).toContext
      _ <- store.put(undertaking).toContext
    } yield undertaking

}
