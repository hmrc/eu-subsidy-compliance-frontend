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

enum AmendmentType(val code: String):
  case Add    extends AmendmentType("1")
  case Amend  extends AmendmentType("2")
  case Delete extends AmendmentType("3")

object AmendmentTypeCode:

  def code(a: AmendmentType): String =
    a.code

  def from(code: String): Option[AmendmentType] =
    code match
      case "1" => Some(AmendmentType.Add)
      case "2" => Some(AmendmentType.Amend)
      case "3" => Some(AmendmentType.Delete)
      case _   => None


  given Format[AmendmentType] = new Format[AmendmentType]:

    override def reads(json: JsValue): JsResult[AmendmentType] =
      json match
        case JsString(value) =>
          AmendmentTypeCode.from(value) match
            case Some(v) => JsSuccess(v)
            case None =>
              JsError(s"Unknown AmendmentType code: $value")

        case other =>
          JsError(s"Expected string AmendmentType, got $other")

    override def writes(o: AmendmentType): JsValue =
      JsString(AmendmentTypeCode.code(o))