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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{Journey, Store, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._

import scala.concurrent.{ExecutionContext, Future}

trait FormHelpers {

  protected val store: Store
  protected implicit val executionContext: ExecutionContext

  protected def processFormSubmission(f: Journey => OptionT[Future, Result])(implicit e: EORI): Future[Result] =
    store.get[SubsidyJourney].toContext
      .flatMap(f)
      .getOrElse(throw new IllegalStateException("Missing journey data - unable to process form submission"))

}
