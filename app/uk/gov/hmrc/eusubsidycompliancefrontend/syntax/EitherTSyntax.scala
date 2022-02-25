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

import cats.data.EitherT

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error

object EitherTSyntax {

  implicit class FutureEitherToEitherTOps[A](val f: Future[Either[Error, A]]) extends AnyVal {
    def toEitherTContext: EitherT[Future, Error, A] = EitherT[Future, Error, A](f)
  }

  implicit class FutureToEitherTOps[A](val f: Future[A]) extends AnyVal {
    def toEitherTContext(implicit ec: ExecutionContext): EitherT[Future, Error, A] = EitherT[Future, Error, A](f
      .map(v => Right(v))
      .recover { case e => Left(Error(e))})
  }

  implicit class EitherToEitherTOps[A](val e: Either[Error, A]) extends AnyVal {
    def toEitherTContext(implicit ec: ExecutionContext): EitherT[Future, Error, A] = EitherT.fromEither(e)
  }

  implicit class OptionToEitherTOps[A](val opt: Option[A]) extends AnyVal {
    def toEitherTContext(implicit ec: ExecutionContext): EitherT[Future, Error, A] = EitherT.fromOption(opt, Error("Missing value"))
  }


}
