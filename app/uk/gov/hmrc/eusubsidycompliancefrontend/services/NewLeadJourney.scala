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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._

case class NewLeadJourney(selectNewLead: FormPage[EORI] = FormPage("select-new-lead")) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    NewLeadJourney.
      unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])

}

object NewLeadJourney {
  import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.eoriFormat
  implicit val formPageEoriFormat: OFormat[FormPage[EORI]] =
    Json.format[FormPage[EORI]]

  implicit val format: Format[NewLeadJourney] =
    Json.format[NewLeadJourney]
}
