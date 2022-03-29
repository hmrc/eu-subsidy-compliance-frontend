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

  // Has user clicked on the check box to do nil return submission
  def hasNilJourneyStarted: Boolean = nilReturnCounter == 1

  // Means the page previous to current page was to submit the nil return
  def isNilJourneyDoneRecently: Boolean = nilReturnCounter == 2

  def setNilReturnValues(b: Boolean): NilReturnJourney =
    this.copy(nilReturn = nilReturn.copy(value = Some(b)), nilReturnCounter = 1)

  def incrementNilReturnCounter: NilReturnJourney = this.copy(nilReturnCounter = nilReturnCounter + 1)

  /**
   * nilReturnCounter is used to track when user has moved to home account page after nil return claim
   *
   * This counter also helps in identifying when to display the success message which should be displayed only once
   *
   * By default,  NilReturnJourney has counter set to 0. When user submit on No claim page, the count is set to 1,
   * indicating user has started the nil return journey.
   *
   * Home account has logic to update the counter only if hasNilJourneyStarted or isNilJourneyDoneRecently.
   *
   * Since the journey has started, when the user is redirected to home account, counter get updated to 2.
   *
   * Home page has logic to display the message only if the isNilJourneyDoneRecently . At this point the message will be
   * displayed. If user refreshes or return to home page via another journey, counter is updated and success message is
   * no longer displayed. Since neither the nil journey has just started nor finished recently, counter will no longer
   * be updated because of this func logic and will be reset to 1 if user goes on to nil return journey again.
   */
  def canIncrementNilReturnCounter: Boolean = hasNilJourneyStarted || isNilJourneyDoneRecently
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
