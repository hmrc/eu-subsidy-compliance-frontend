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

trait BigDecimalCodec[A]:

  def name: String

  def validate(value: BigDecimal): A

  extension (a: A) def value: BigDecimal

  given Format[A] = new Format[A]:

    override def reads(json: JsValue): JsResult[A] =
      json match
        case JsNumber(value) =>
          try JsSuccess(validate(value))
          catch
            case _: IllegalArgumentException =>
              JsError(s"Expected a valid $name, got $value instead.")

        case xs =>
          JsError(
            JsPath ->
              JsonValidationError(
                s"Expected a valid $name, got $xs instead."
              )
          )

    override def writes(o: A): JsValue =
      JsNumber(o.value)
