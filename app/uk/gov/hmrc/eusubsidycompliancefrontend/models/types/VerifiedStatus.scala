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

enum VerifiedStatus(val wire: String):
  case Verified extends VerifiedStatus("verified")

object VerifiedStatus:
  given Format[VerifiedStatus] = new Format[VerifiedStatus]:

    override def reads(json: JsValue): JsResult[VerifiedStatus] =
      json match
        case JsString("verified") =>
          JsSuccess(VerifiedStatus.Verified)

        case JsString(other) =>
          JsError(s"Unknown VerifiedStatus: $other")

        case other =>
          JsError(s"Expected string VerifiedStatus, got $other")

    override def writes(o: VerifiedStatus): JsValue =
      JsString(o.wire)
