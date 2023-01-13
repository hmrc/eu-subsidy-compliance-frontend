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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney.Forms.NilReturnFormPage

case class NilReturnJourney(nilReturn: NilReturnFormPage = NilReturnFormPage(), displayNotification: Boolean = false)
    extends Journey {
  override def steps: Array[FormPage[_]] = Array(nilReturn)

}

object NilReturnJourney {
  implicit val format: Format[NilReturnJourney] = Json.format[NilReturnJourney]

  object Forms {

    private val controller = routes.NoClaimNotificationController

    case class NilReturnFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getNoClaimNotification.url
    }

    object NilReturnFormPage {
      implicit val nilReturnFormPageFormat: OFormat[NilReturnFormPage] = Json.format
    }
  }

}
