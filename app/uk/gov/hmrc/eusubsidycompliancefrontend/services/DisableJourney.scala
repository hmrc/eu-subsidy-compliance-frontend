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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.DisableJourney.FormPages.DisableUndertakingFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form

import java.time.LocalDateTime

case class DisableJourney(
  disableUndertaking: DisableUndertakingFormPage = DisableUndertakingFormPage(),
  disableBy: Option[LocalDateTime] = None
) extends Journey {
  override def steps: Array[FormPage[_]] =
    Array(
      disableUndertaking
    )

  def setDisableFlag(boolean: Boolean): DisableJourney =
    this.copy(disableUndertaking = disableUndertaking.copy(value = Some(boolean)))

  def setDisableBy(date: LocalDateTime): DisableJourney = this.copy(disableBy = Some(date))

  def isDisabled(currentTime: LocalDateTime) =
    disableUndertaking.value.contains(true) && disableBy.fold(false)(_.isAfter(currentTime))
}

object DisableJourney {
  implicit val format: Format[DisableJourney] = Json.format[DisableJourney]

  object FormPages {
    private val controller = routes.UndertakingController

    case class DisableUndertakingFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getDisableUndertakingConfirm().url
    }

    object DisableUndertakingFormPage {
      implicit val disableUndertakingFormPageFormat: OFormat[DisableUndertakingFormPage] = Json.format
    }

  }
}
