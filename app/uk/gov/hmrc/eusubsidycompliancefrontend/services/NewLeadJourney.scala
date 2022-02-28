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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NewLeadJourney.Forms.SelectNewLeadFormPage

case class NewLeadJourney(selectNewLead: SelectNewLeadFormPage = SelectNewLeadFormPage()) extends Journey {

  override def steps: List[FormPageBase[_]] = List(
    selectNewLead
  )

}

object NewLeadJourney {
  import Journey._ // N.B. don't let intellij delete this

  implicit val formPageEoriFormat: OFormat[FormPage[EORI]] = Json.format[FormPage[EORI]]
  implicit val format: Format[NewLeadJourney] = Json.format[NewLeadJourney]

  object FormUrls {
    val SelectNewLead = "select-new-lead"
  }

  object Forms {
    // TODO - replace uris with routes lookups
    case class SelectNewLeadFormPage(value: Form[EORI] = None) extends FormPageBase[EORI] { val uri = FormUrls.SelectNewLead }
    object SelectNewLeadFormPage { implicit val selectNewLeadFormPageFormat: OFormat[SelectNewLeadFormPage] = Json.format }
  }

}
