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
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{Store, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._

import scala.concurrent.{ExecutionContext, Future}

trait FormPageSupport {

  protected val store: Store
  protected implicit val executionContext: ExecutionContext

  protected def renderFormIfEligible(f: SubsidyJourney => Result)(implicit r: AuthenticatedEscRequest[AnyContent]): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber

    store.get[SubsidyJourney].toContext
      .map { journey =>
        if (journey.isEligibleForStep) f(journey)
        else Redirect(journey.previous)
      }
      .getOrElse(Redirect(routes.SubsidyController.getReportPayment().url))

  }

  protected def processFormSubmission(f: SubsidyJourney => OptionT[Future, Result])(implicit e: EORI): Future[Result] =
    store.get[SubsidyJourney].toContext
      .flatMap(f)
      .getOrElse(Redirect(routes.SubsidyController.getReportPayment().url))

}
