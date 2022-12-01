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

package uk.gov.hmrc.eusubsidycompliancefrontend.journeys

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

case class BusinessEntityJourney(
  addBusiness: AddBusinessFormPage = AddBusinessFormPage(),
  eori: AddEoriFormPage = AddEoriFormPage(),
  isLeadSelectJourney: Option[Boolean] = None,
  oldEORI: Option[EORI] = None
) extends Journey {

  override def steps: Array[FormPage[_]] =
    Array(
      addBusiness,
      eori,
    )

  def isAmend: Boolean = oldEORI.nonEmpty
  def onLeadSelectJourney: Boolean = isLeadSelectJourney.contains(true)

  def setEori(e: EORI): BusinessEntityJourney =
    this.copy(
      eori = eori.copy(value = e.some),
      oldEORI = eori.value
    )

  def setAddBusiness(b: Boolean): BusinessEntityJourney =
    this.copy(addBusiness = addBusiness.copy(value = b.some))

}

object BusinessEntityJourney {

  implicit val format: Format[BusinessEntityJourney] = Json.format[BusinessEntityJourney]

  object FormPages {

    private val controller = routes.BusinessEntityController

    case class AddBusinessFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getAddBusinessEntity.url
    }
    case class AddEoriFormPage(value: Form[EORI] = None) extends FormPage[EORI] {
      def uri = controller.getEori.url
    }

    object AddBusinessFormPage { implicit val addBusinessFormPageFormat: OFormat[AddBusinessFormPage] = Json.format }
    object AddEoriFormPage { implicit val addEoriFormPageFormat: OFormat[AddEoriFormPage] = Json.format }

  }

}
