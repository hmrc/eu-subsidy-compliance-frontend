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

package uk.gov.hmrc.eusubsidycompliancefrontend.syntax

import cats.data.OptionT
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Much of our code operates with Future[Option[_]] types which can benefit from using OptionT to simplify unpacking
  * the results.
  *
  * This syntax provides some convenience extension methods to clean up OptionT related boilerplate when values need to
  * be 'lifted' into the Future[Option[_]] context.
  *
  * In the long term we should revisit our error handling and consider using Future[Either[_, _]] instead of throwing
  * exceptions.
  *
  * This is a step in that direction and introduces the idea of lifting values into a context, where for now, that
  * context is Future[Option[_]]
  */
object OptionTSyntax {

  implicit class FutureToOptionTOps[A](val f: Future[A]) extends AnyVal {
    def toContext(implicit ec: ExecutionContext): OptionT[Future, A] = OptionT[Future, A](f.map(Some(_)))
  }

  implicit class ValueToOptionTOps[A](val a: A) extends AnyVal {
    def toContext: OptionT[Future, A] = OptionT(Future.successful(Option(a)))
  }

  implicit class OptionToOptionTOps[A](val o: Option[A]) extends AnyVal {
    def toContext(implicit ec: ExecutionContext): OptionT[Future, A] = OptionT.fromOption[Future](o)
  }

  implicit class FutureOptionToOptionTOps[A](val fo: Future[Option[A]]) extends AnyVal {
    def toContext: OptionT[Future, A] = OptionT(fo)
  }

}
