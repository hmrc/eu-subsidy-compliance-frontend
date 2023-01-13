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

import cats.data.OptionT
import play.api.libs.json.Reads
import play.api.mvc.Result
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait ControllerFormHelpers {

  protected val store: Store
  protected implicit val executionContext: ExecutionContext

  protected def processFormSubmission[A : ClassTag](f: A => OptionT[Future, Result])(implicit e: EORI, r: Reads[A]): Future[Result] =
    store.get[A].toContext
      .flatMap(f)
      .getOrElse(throw new IllegalStateException("Missing journey data - unable to process form submission"))


}
