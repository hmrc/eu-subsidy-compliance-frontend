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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.FormPages._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form

case class BecomeLeadJourney(
  becomeLeadEori: BecomeLeadEoriFormPage = BecomeLeadEoriFormPage(),
  acceptResponsibilities: AcceptResponsibilitiesFormPage = AcceptResponsibilitiesFormPage(),
  confirmation: ConfirmationFormPage = ConfirmationFormPage()
) extends Journey {

  override def steps: Array[FormPage[_]] =
    Array(
      acceptResponsibilities,
      becomeLeadEori,
      confirmation
    )

  def setAcceptResponsibilities(value: Boolean): BecomeLeadJourney = this.copy(
    acceptResponsibilities = acceptResponsibilities.copy(value = value.some)
  )

}

object BecomeLeadJourney {

  implicit val format: Format[BecomeLeadJourney] = Json.format[BecomeLeadJourney]

  object FormPages {

    private val controller = routes.BecomeLeadController

    case class BecomeLeadEoriFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getBecomeLeadEori().url
    }
    case class AcceptResponsibilitiesFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getAcceptResponsibilities().url
    }
    case class ConfirmationFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getPromotionConfirmation().url
    }

    object BecomeLeadEoriFormPage {
      implicit val becomeLeadEoriFormPageFormat: OFormat[BecomeLeadEoriFormPage] = Json.format
    }
    object AcceptResponsibilitiesFormPage {
      implicit val acceptResponsibilitiesFormPageFormat: OFormat[AcceptResponsibilitiesFormPage] = Json.format
    }
    object ConfirmationFormPage { implicit val confirmationFormPageFormat: OFormat[ConfirmationFormPage] = Json.format }

  }

}
