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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

final case class Error(value: Either[String, Throwable]) extends AnyVal

object Error {

  def apply(message: String): Error = Error(Left(message))

  def apply(error: Throwable): Error = Error(Right(error))

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  implicit class ErrorOps(e: Error) {
    def doThrow(message: String): Nothing =
      e.value.fold(info => sys.error(s"$message::$info"), ex => throw new RuntimeException(message, ex))
  }
}

