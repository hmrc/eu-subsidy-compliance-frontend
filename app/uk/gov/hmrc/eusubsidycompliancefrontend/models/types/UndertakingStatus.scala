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
  case Active extends UndertakingStatus(0)
  case SuspendedAutomated extends UndertakingStatus(1)
  case SuspendedBen extends UndertakingStatus(2)
  case SuspendedManual extends UndertakingStatus(5)
  case SuspendedUndertaking extends UndertakingStatus(8)
  case Inactive extends UndertakingStatus(9)

object UndertakingStatus:

  private val byName: Map[String, UndertakingStatus] =
    Map(
      "active" -> Active,
      "suspendedAutomated" -> SuspendedAutomated,
      "suspendedBen" -> SuspendedBen,
      "suspendedManual" -> SuspendedManual,
      "suspendedUndertaking" -> SuspendedUndertaking,
      "inactive" -> Inactive
    )
  private val byCode: Map[Int, UndertakingStatus] =
    values.map(s => s.code -> s).toMap

  def fromCode(code: Int): UndertakingStatus =
    byCode(code)

  given Format[UndertakingStatus] = Format(
    Reads {
      case JsString(name) =>
        byName
          .get(name)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown UndertakingStatus: $name"))

      case _ =>
        JsError("Expected UndertakingStatus as a string")
    },
    Writes {
      case Active => JsString("active")
      case SuspendedAutomated => JsString("suspendedAutomated")
      case SuspendedBen => JsString("suspendedBen")
      case SuspendedManual => JsString("suspendedManual")
      case SuspendedUndertaking => JsString("suspendedUndertaking")
      case Inactive => JsString("inactive")
    }
  )
