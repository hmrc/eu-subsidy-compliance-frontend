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

package uk.gov.hmrc.eusubsidycompliancefrontend.syntax

import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.http.HttpResponse

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object HttpResponseSyntax {

  trait ResponseParsingLogger[A, B] {

    def logSuccess(a: B): Unit

    def logValidationFailure(
      response: HttpResponse,
      validationFailures: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
    ): Unit

    def logParsingFailure(response: HttpResponse, throwable: Throwable): Unit
  }

  implicit class HttpResponseOps[ResponseError](private val response: HttpResponse) extends AnyVal {

    /**
      * For now logging can be handled by a type class (version of an adaptor pattern passed often implicitly, such as sort stuff)
      *  as going from no parenthesis to some will cause a lot of call changes and I want to limit noise in this PR.
      *
      *  Due to GPDR etc we need to be picky what we log? Better to be safe than sorry
      *
      * @param reads
      * @tparam A
      * @return
      */
    def parseJSON[A](implicit
      reads: Reads[A],
      maybeLogger: Option[ResponseParsingLogger[ResponseError, A]] = None,
      classTag: ClassTag[
        A
      ] // so we can keep track of what generic type we are using, generics gets type erased by the JVM
    ): Either[String, A] =
      Try(response.json) match {
        case Success(jsValue) =>
          jsValue
            .validate[A]
            .fold[Either[String, A]](
              (validationFailures: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =>
                processFailedValidation(maybeLogger, validationFailures),
              (successValue: A) => processSuccessfulParsing(maybeLogger, successValue)
            )

        case Failure(error: Throwable) =>
          maybeLogger.foreach(_.logParsingFailure(response, error))
          Left(s"Could not read http response as valid expected JSON format for $classTag")
      }

    private def processFailedValidation[A](
      maybeLogger: Option[ResponseParsingLogger[ResponseError, A]],
      validationFailures: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
    ): Left[String, Nothing] = {
      maybeLogger.foreach(_.logValidationFailure(response, validationFailures))
      Left("Could not parse http response JSON")
    }

    private def processSuccessfulParsing[A](
      maybeLogger: Option[ResponseParsingLogger[ResponseError, A]],
      result: A
    ): Either[String, A] = {
      maybeLogger.foreach(_.logSuccess(result))
      Right(result)
    }
  }

}
