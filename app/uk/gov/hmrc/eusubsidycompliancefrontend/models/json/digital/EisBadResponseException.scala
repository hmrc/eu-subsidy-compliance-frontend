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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.json.digital

import java.time.ZonedDateTime

import uk.gov.hmrc.eusubsidycompliancefrontend.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EisParamName.EisParamName
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EisParamName, EisParamValue}

class EisBadResponseException(
  status: String,
  processingDate: ZonedDateTime,
  statusText: Option[String],
  returnParameters: Option[List[Params]]
) extends RuntimeException(
  s"$processingDate $status ${statusText.getOrElse("")} ${returnParameters.getOrElse(List.empty[Params])}"
) {

  val params: Map[EisParamName, EisParamValue] = returnParameters.getOrElse(List.empty[Params]).map(x => (x.paramName, x.paramValue)).toMap
  val code: EisParamValue =  params.getOrElse(EisParamName.ERRORCODE, EisParamValue("UNKNOWN"))
  val message: EisParamValue = params.getOrElse(EisParamName.ERRORTEXT, EisParamValue("UNKNOWN"))

}
