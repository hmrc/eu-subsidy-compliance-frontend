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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NilReturnJourney.Forms.NilReturnFormPage

case class NilReturnJourney(nilReturn: NilReturnFormPage = NilReturnFormPage(), nilReturnCounter: Int = 0)
    extends Journey {
  override def steps: Array[FormPage[_]] = Array(nilReturn)

  def hasNilJourneyStarted = nilReturnCounter == 1 //means user has clicked on the check box to do nil return submission
  def isNilJourneyDoneRecently =
    nilReturnCounter == 2 //means the page previous to current page was to submit the nil return
}

object NilReturnJourney {
  implicit val format: Format[NilReturnJourney] = Json.format[NilReturnJourney]

  object Forms {

    private val controller = routes.NoClaimNotificationController

    case class NilReturnFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getNoClaimNotification().url
    }

    object NilReturnFormPage {
      implicit val nilReturnFormPageFormat: OFormat[NilReturnFormPage] = Json.format
    }

  }

}
