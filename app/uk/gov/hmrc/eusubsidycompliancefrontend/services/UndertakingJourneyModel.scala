/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

case class UndertakingJourneyModel(
  // TODO model each step in the journey
  eori: EORI
) extends Journey {

  // TODO override Journey where needed e.g. steps, n.g. this is where you need shapeless for toList
  override def steps: List[Option[FormPage[_]]] = List.empty

}

object UndertakingJourneyModel {
  implicit val format: Format[UndertakingJourneyModel] = Json.format[UndertakingJourneyModel]
}
