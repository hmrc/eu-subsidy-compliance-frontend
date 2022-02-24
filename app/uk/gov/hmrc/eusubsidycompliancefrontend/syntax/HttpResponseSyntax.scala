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

import play.api.libs.json.Reads
import uk.gov.hmrc.http.HttpResponse

import scala.util.{Failure, Success, Try}

object HttpResponseSyntax {

  implicit class HttpResponseOps(private val response: HttpResponse) extends AnyVal {

    def parseJSON[A](implicit reads: Reads[A]): Either[String, A] =
      Try(response.json) match {
        case Success(jsValue) ⇒
          jsValue
            .validate[A]
            .fold[Either[String, A]](
              _ ⇒
                // there was JSON in the response but we couldn't read it
                Left("Could not parse http response JSON"),
              Right(_)
            )

        case Failure(_) ⇒
          // response.json failed in this case - there was no JSON in the response
          Left(s"Could not read http response as JSON")
      }

  }
}
