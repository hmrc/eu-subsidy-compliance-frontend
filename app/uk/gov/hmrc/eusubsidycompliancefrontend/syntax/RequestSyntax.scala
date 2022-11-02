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

import play.api.mvc.Request

import java.net.URI

object RequestSyntax {

  private val Referer = "Referer"

  implicit class RequestOps[A](val request: Request[A]) extends AnyVal {

    def isFrom(url: String): Boolean = request.headers.get(Referer).exists(_.endsWith(url))

    def isLocal: Boolean = request.domain == "localhost"

    def toRedirectTarget: String = {
      val res = if (isLocal) new URI(s"http://${request.host}${request.uri}").toString
      else request.uri
      println(s"toRedirectTarget - built path: $res")
      res
    }

    def toRedirectTarget(path: String): String = {
      val res = if (isLocal) new URI(s"http://${request.host}$path").toString
      else path
      println(s"toRedirectTarget(path) - built path: $res")
      res
    }

  }

}
