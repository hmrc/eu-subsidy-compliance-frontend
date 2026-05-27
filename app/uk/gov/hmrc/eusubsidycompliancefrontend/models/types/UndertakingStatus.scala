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

enum UndertakingStatus(val code: Int):
  case Active               extends UndertakingStatus(0)
  case SuspendedAutomated   extends UndertakingStatus(1)
  case SuspendedBen         extends UndertakingStatus(2)
  case SuspendedManual      extends UndertakingStatus(5)
  case SuspendedUndertaking extends UndertakingStatus(8)
  case Inactive             extends UndertakingStatus(9)

object UndertakingStatus:

  private val byCode: Map[Int, UndertakingStatus] =
    values.map(s => s.code -> s).toMap

  def fromCode(code: Int): Option[UndertakingStatus] =
    byCode.get(code)
  
  given Format[UndertakingStatus] = new Format[UndertakingStatus]:

    override def reads(json: JsValue): JsResult[UndertakingStatus] =
      json match
        case JsNumber(n) =>
          UndertakingStatus.fromCode(n.toIntExact) match
            case Some(v) => JsSuccess(v)
            case None => JsError(s"Unknown UndertakingStatus code: $n")

        case other =>
          JsError("Expected numeric UndertakingStatus")

    override def writes(o: UndertakingStatus): JsValue =
      JsNumber(o.code)