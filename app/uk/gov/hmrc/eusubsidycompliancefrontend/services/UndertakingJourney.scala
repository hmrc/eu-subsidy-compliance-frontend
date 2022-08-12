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

import cats.implicits._
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.{Form, Uri}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.UndertakingJourney.Forms.{UndertakingConfirmationFormPage, UndertakingCyaFormPage, UndertakingNameFormPage, UndertakingSectorFormPage, UndertakingConfirmEmailFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

case class UndertakingJourney(
                               name: UndertakingNameFormPage = UndertakingNameFormPage(),
                               sector: UndertakingSectorFormPage = UndertakingSectorFormPage(),
                               verifiedEmail: UndertakingConfirmEmailFormPage = UndertakingConfirmEmailFormPage(),
                               cya: UndertakingCyaFormPage = UndertakingCyaFormPage(),
                               confirmation: UndertakingConfirmationFormPage = UndertakingConfirmationFormPage(),
                               isAmend: Boolean = false
) extends Journey {

  override def steps = Array(
    name,
    sector,
    verifiedEmail,
    cya,
    confirmation
  )
  private lazy val previousMap: Map[String, Uri] = Map(
    routes.UndertakingController.getUndertakingName().url -> routes.EligibilityController.getCreateUndertaking().url,
    routes.UndertakingController.getSector().url -> routes.UndertakingController.getUndertakingName().url
  )

  override def previous(implicit r: Request[_]): Uri =
    if (isAmend) routes.UndertakingController.getAmendUndertakingDetails().url
    else previousMap.getOrElse(r.uri, super.previous)

  override def next(implicit request: Request[_]): Future[Result] =
    if (isAmend) Redirect(routes.UndertakingController.getAmendUndertakingDetails()).toFuture
    else if (requiredDetailsProvided) Redirect(routes.UndertakingController.getCheckAnswers()).toFuture
    else super.next

  def isEmpty: Boolean = steps.flatMap(_.value).isEmpty

  def isCurrentPageCYA(implicit request: Request[_]): Boolean =
    request.uri == routes.UndertakingController.getCheckAnswers().url

  private def requiredDetailsProvided = Seq(name, sector, verifiedEmail).map(_.value.isDefined) == Seq(true, true, true)

  def setUndertakingName(n: String): UndertakingJourney = this.copy(name = name.copy(value = Some(n)))

  def setUndertakingSector(s: Int): UndertakingJourney = this.copy(sector = sector.copy(value = Some(Sector(s))))

  def setUndertakingCYA(b: Boolean): UndertakingJourney = this.copy(cya = cya.copy(value = Some(b)))

  def setVerifiedEmail(e: String): UndertakingJourney = this.copy(verifiedEmail = verifiedEmail.copy(value = Some(e)))

  def setUndertakingConfirmation(b: Boolean): UndertakingJourney =
    this.copy(confirmation = confirmation.copy(value = Some(b)))

}

object UndertakingJourney {

  implicit val format: Format[UndertakingJourney] = Json.format[UndertakingJourney]

  def fromUndertaking(undertaking: Undertaking): UndertakingJourney = UndertakingJourney(
    name = UndertakingNameFormPage(undertaking.name.some),
    sector = UndertakingSectorFormPage(undertaking.industrySector.some)
  )

  object Forms {

    private val controller = routes.UndertakingController

    case class UndertakingNameFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getUndertakingName().url
    }
    case class UndertakingSectorFormPage(value: Form[Sector] = None) extends FormPage[Sector] {
      def uri = controller.getSector().url
    }
    case class UndertakingConfirmEmailFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getConfirmEmail().url
    }
    case class UndertakingCyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCheckAnswers().url
    }
    case class UndertakingConfirmationFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.postConfirmation().url
    }

    object UndertakingNameFormPage {
      implicit val undertakingNameFormPage: OFormat[UndertakingNameFormPage] = Json.format
    }
    object UndertakingSectorFormPage {
      implicit val undertakingSectorFormPage: OFormat[UndertakingSectorFormPage] = Json.format
    }
    object UndertakingConfirmEmailFormPage { implicit val confirmEmailFormPageFormat: OFormat[UndertakingConfirmEmailFormPage] = Json.format }
    object UndertakingCyaFormPage { implicit val undertakingCyaFormPage: OFormat[UndertakingCyaFormPage] = Json.format }
    object UndertakingConfirmationFormPage {
      implicit val undertakingConfirmationFormPage: OFormat[UndertakingConfirmationFormPage] = Json.format
    }

  }
}
