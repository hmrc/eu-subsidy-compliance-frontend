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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector

case class UndertakingJourney(
  eoriCheck: FormPage[Boolean] = FormPage("eoricheck"), // TODO - double check thinking, maybe part of eligibility
  signOut: FormPage[Boolean] = FormPage("incorrect-eori"),
  createUndertaking: FormPage[Boolean] = FormPage("create-undertaking"),
  name: FormPage[String] = FormPage("undertaking-name"),
  sector: FormPage[Sector] = FormPage("sector")

) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    UndertakingJourney.
      unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])
      .filterNot(x => x.fold(false) { y =>
        y.uri === "incorrect-eori" &&
          this.eoriCheck.value.getOrElse(false)
      })

}

object UndertakingJourney {

  import Journey._
  import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.format
  implicit val formPageSectorFormat: OFormat[FormPage[Sector]] =
    Json.format[FormPage[Sector]]

  implicit val format: Format[UndertakingJourney] = Json.format[UndertakingJourney]
}