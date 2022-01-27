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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.emailSend

import ai.x.play.json.Jsonx
import play.api.libs.json.{Format, JsObject, JsResult, JsValue, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName, UndertakingRef}

sealed trait EmailParameterType

object EmailParameterType {

  case object  SingleEORI extends EmailParameterType
  case object DoubleEORI extends EmailParameterType
  case object DoubleEORIAndDate extends EmailParameterType
  case object  SingleEORIAndDate extends EmailParameterType

  import ai.x.play.json.SingletonEncoder.simpleName
  import ai.x.play.json.implicits.formatSingleton
  implicit val format: Format[EmailParameterType] = Jsonx.formatSealed[EmailParameterType]
}

trait EmailParameters {
  val undertakingName: UndertakingName
  val undertakingRef: UndertakingRef
  val description: String
  val emailParameterType: EmailParameterType
}

object EmailParameters {

  final case class SingleEORIEmailParameter(eori: EORI, undertakingName: UndertakingName, undertakingRef: UndertakingRef, description: String) extends EmailParameters {
    val emailParameterType = EmailParameterType.SingleEORI
  }

  final case class DoubleEORIEmailParameter(eori: EORI, beEORI: EORI,  undertakingName: UndertakingName, undertakingRef: UndertakingRef, description: String) extends EmailParameters {
    val emailParameterType = EmailParameterType.DoubleEORI
  }

  final case class SingleEORIAndDateEmailParameter(eori: EORI, undertakingName: UndertakingName, undertakingRef: UndertakingRef, effectiveDate: String, description: String) extends EmailParameters {
    val emailParameterType = EmailParameterType.SingleEORIAndDate
  }

  final case class DoubleEORIAndDateEmailParameter(eori: EORI, beEORI: EORI, undertakingName: UndertakingName, undertakingRef: UndertakingRef, effectiveDate: String, description: String) extends EmailParameters {
    val emailParameterType = EmailParameterType.DoubleEORIAndDate
  }

  implicit val format: OFormat[EmailParameters] = new OFormat[EmailParameters] {
    override def writes(o: EmailParameters): JsObject = {
      val json  = o match {
        case s: SingleEORIEmailParameter => Json.writes[SingleEORIEmailParameter].writes(s)
        case d: DoubleEORIEmailParameter => Json.writes[DoubleEORIEmailParameter].writes(d)
        case sd: SingleEORIAndDateEmailParameter => Json.writes[SingleEORIAndDateEmailParameter].writes(sd)
        case dd: DoubleEORIAndDateEmailParameter => Json.writes[DoubleEORIAndDateEmailParameter].writes(dd)
      }
      json ++ Json.obj("emailParameterType" -> o.emailParameterType)
    }

    override def reads(json: JsValue): JsResult[EmailParameters] =
      (json \ "emailParameterType")
        .validate[EmailParameterType]
        .flatMap {
          case EmailParameterType.SingleEORI => Json.reads[SingleEORIEmailParameter].reads(json)
          case EmailParameterType.DoubleEORI => Json.reads[DoubleEORIEmailParameter].reads(json)
          case EmailParameterType.SingleEORIAndDate => Json.reads[SingleEORIAndDateEmailParameter].reads(json)
          case EmailParameterType.DoubleEORIAndDate => Json.reads[DoubleEORIAndDateEmailParameter].reads(json)
        }
  }


}

