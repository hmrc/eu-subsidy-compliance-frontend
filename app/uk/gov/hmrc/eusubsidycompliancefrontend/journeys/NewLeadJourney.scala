/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NewLeadJourney.Forms.SelectNewLeadFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

case class NewLeadJourney(selectNewLead: SelectNewLeadFormPage = SelectNewLeadFormPage()) extends Journey {
  override def steps: Array[FormPage[_]] = Array(selectNewLead)
}

object NewLeadJourney {

  implicit val format: Format[NewLeadJourney] = Json.format[NewLeadJourney]

  object Forms {

    private val controller = routes.SelectNewLeadController

    case class SelectNewLeadFormPage(value: Form[EORI] = None) extends FormPage[EORI] {
      def uri = controller.getSelectNewLead.url
    }

    object SelectNewLeadFormPage {
      implicit val selectNewLeadFormPageFormat: OFormat[SelectNewLeadFormPage] = Json.format
    }

  }

}
