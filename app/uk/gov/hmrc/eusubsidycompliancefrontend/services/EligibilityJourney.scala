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

import cats.implicits._

import play.api.libs.json.{Format, Json, OFormat}
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._

case class EligibilityJourney(
  customsWaivers: FormPage[Boolean] = FormPage("do-you-claim-customs-waivers"),
  willYouClaim: FormPage[Boolean] = FormPage("will-you-claim-customs-waivers"),
  mainBusinessCheck: FormPage[Boolean] = FormPage("main-business-check")
) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    EligibilityJourney
    .unapply(this)
    .map(_.toList)
    .fold(List.empty[Any])(identity)
    .map(_.cast[FormPage[_]])
      .filterNot(x => x.fold(false){ y => // bit hacky but this is how we deal with branching
        y.uri === "will-you-claim-customs-waivers" && this.customsWaivers.value.getOrElse(false)
      })

}

object EligibilityJourney {
  // TODO consider lift to Journey
  implicit val formPageBooleanValueFormat: OFormat[FormPage[Boolean]] =
    Json.format[FormPage[Boolean]]

  implicit val format: Format[EligibilityJourney] = Json.format[EligibilityJourney]
}


