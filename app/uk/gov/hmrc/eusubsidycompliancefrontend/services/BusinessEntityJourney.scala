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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ContactDetails, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._

case class BusinessEntityJourney(
  addBusiness: FormPage[Boolean] = FormPage("add-member"),
  eori: FormPage[EORI] = FormPage("add-business-entity-eori"),
  contact: FormPage[ContactDetails] = FormPage("add-business-entity-contact"),
  cya: FormPage[Boolean] = FormPage("check-your-answers-businesses") // TODO
) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    BusinessEntityJourney.
      unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])

}

object BusinessEntityJourney {

  // TODO populate the Journey[s] from the undertaking, probably need to map them by eori
  def fromUndertakingOpt(undertakingOpt: Option[Undertaking]): BusinessEntityJourney = BusinessEntityJourney()

  def businessEntityJourneyForEori(undertakingOpt: Option[Undertaking], eori: EORI): BusinessEntityJourney = {
    undertakingOpt match {
      case Some(undertaking) =>
        val empty = BusinessEntityJourney()
        val b: Option[BusinessEntity] = undertaking.undertakingBusinessEntity.find(_.businessEntityIdentifier == eori)
        val cd: Option[ContactDetails] = b.flatMap(_.contacts)
        empty.copy(
          empty.addBusiness.copy(value = Some(true)),
          empty.eori.copy(value = Some(eori)),
          empty.contact.copy(value = cd)
        )
      // TODO - what is the correct behaviour here?
      case None => BusinessEntityJourney()
    }
  }

  import Journey._ // N.B. don't let intellij delete this
  import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.eoriFormat
  implicit val formPageEoriFormat: OFormat[FormPage[EORI]] =
    Json.format[FormPage[EORI]]
  implicit val formatContactDetails: OFormat[ContactDetails] =
    Json.format[ContactDetails]
  implicit val formPageContactFormat: OFormat[FormPage[ContactDetails]] =
    Json.format[FormPage[ContactDetails]]

  implicit val format: Format[BusinessEntityJourney] =
    Json.format[BusinessEntityJourney]

}