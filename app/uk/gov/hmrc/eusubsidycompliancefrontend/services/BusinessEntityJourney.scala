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

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.Forms.{AddBusinessCyaFormPage, AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form

case class BusinessEntityJourney(
  addBusiness: AddBusinessFormPage = AddBusinessFormPage(),
  eori: AddEoriFormPage = AddEoriFormPage(),
  cya: AddBusinessCyaFormPage = AddBusinessCyaFormPage(),
  isLeadSelectJourney: Option[Boolean] = None
) extends Journey {

  override protected def steps: List[FormPage[_]] =
    List(
      addBusiness,
      eori,
      cya,
    )
}

object BusinessEntityJourney {

  implicit val format: Format[BusinessEntityJourney] = Json.format[BusinessEntityJourney]

  // TODO populate the Journey[s] from the undertaking, probably need to map them by eori
  def fromUndertakingOpt(undertakingOpt: Option[Undertaking]): BusinessEntityJourney = BusinessEntityJourney()

  def businessEntityJourneyForEori(undertakingOpt: Option[Undertaking], eori: EORI): BusinessEntityJourney = {
    undertakingOpt.fold(BusinessEntityJourney()) { _ =>
      BusinessEntityJourney(
        addBusiness = AddBusinessFormPage(true.some),
        eori = AddEoriFormPage(eori.some)
      )
    }
  }

  object FormUrls {
    val AddBusiness = "add-member"
    val Eori = "add-business-entity-eori"
    val Cya = "check-your-answers-businesses"
  }

  object Forms {
    case class AddBusinessFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { val uri = FormUrls.AddBusiness }
    case class AddEoriFormPage(value: Form[EORI] = None) extends FormPage[EORI] { val uri = FormUrls.Eori }
    case class AddBusinessCyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { val uri = FormUrls.Cya }

    object AddBusinessFormPage { implicit val addBusinessFormPageFormat: OFormat[AddBusinessFormPage] = Json.format }
    object AddEoriFormPage { implicit val addEoriFormPageFormat: OFormat[AddEoriFormPage] = Json.format }
    object AddBusinessCyaFormPage { implicit val cyaFormPageFormat: OFormat[AddBusinessCyaFormPage] = Json.format }
  }

}
