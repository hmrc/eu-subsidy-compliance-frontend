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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ContactDetails, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.FormUrls._

case class BusinessEntityJourney(
  addBusiness: FormPage[Boolean] = FormPage(AddBusiness),
  eori: FormPage[EORI] = FormPage(Eori),
  contact: FormPage[ContactDetails] = FormPage(Contact),
  cya: FormPage[Boolean] = FormPage(Cya),
  isLeadSelectJourney: Option[Boolean] = None
) extends Journey {

  override protected def steps: List[FormPage[_]] = List(
    addBusiness,
    eori,
    contact,
    cya,
  )

}

object BusinessEntityJourney {

  import Journey._ // N.B. don't let intellij delete this

  implicit val formPageEoriFormat: OFormat[FormPage[EORI]] = Json.format[FormPage[EORI]]
  implicit val formatContactDetails: OFormat[ContactDetails] = Json.format[ContactDetails]
  implicit val formPageContactFormat: OFormat[FormPage[ContactDetails]] = Json.format[FormPage[ContactDetails]]
  implicit val format: Format[BusinessEntityJourney] = Json.format[BusinessEntityJourney]

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


  object FormUrls {
    val AddBusiness = "add-member"
    val Eori = "add-business-entity-eori"
    val Contact = "add-business-entity-contact"
    val Cya = "check-your-answers-businesses"
  }

}