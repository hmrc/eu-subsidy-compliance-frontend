/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.types

import play.api.libs.json.*

import scala.util.matching.Regex

object EORI:
  opaque type EORI = String

  def apply(s: String): EORI = {
    Option(s)
      .map(normalize)
      .filter(valid)
      .getOrElse(throw new IllegalArgumentException(s"$s is not a valid EORI"))
  }

  extension (x: EORI) def value: String = x

  private def valid(s: String): Boolean =
    s.matches("^(GB|XI)[0-9]{12,15}$")

  val Regex: Regex = "^(GB|XI)[0-9]{12,15}$".r

  private def normalize(s: String): String =
    s.trim.toUpperCase

  def withGbPrefix(eori: String): String =
    if eori.startsWith("GB") then eori
    else s"GB$eori"

  def formatEori(eori: String): String =
    val s = eori.replaceAll(" ", "")
    s.take(2).toUpperCase + s.drop(2)

  val ValidLengthsWithPrefix: Set[Int] = Set(14, 17)

  def from(raw: String): Option[EORI] =
    val formatted = formatEori(raw)

    Option(formatted)
      .filter(Regex.matches)

  given Format[EORI] = new Format[EORI]:

    override def reads(json: JsValue): JsResult[EORI] =
      json match
        case JsString(value) =>
          from(value) match
            case Some(v) => JsSuccess(v)
            case None =>
              JsError("Invalid EORI format")

        case other =>
          JsError("Expected string for EORI")

    override def writes(o: EORI): JsValue =
      JsString(o.value)
