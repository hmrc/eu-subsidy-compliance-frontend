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

object BigDecimalCodec:

  def format[A](name: String, from: BigDecimal => A, to: A => BigDecimal): Format[A] =

    new Format[A]:

      override def reads(json: JsValue): JsResult[A] =
        json match
          case JsNumber(v) =>
            from(v) match
              case Some(a: A) => JsSuccess(a)
              case None    => JsError(s"Expected a valid $name, got $v instead.")

          case other =>
            JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $other instead""")))

      override def writes(o: A): JsValue =
        JsNumber(to(o))