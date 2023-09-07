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

import cats.implicits._
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.{Form, Uri}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

case class UndertakingJourney(
  about: AboutUndertakingFormPage = AboutUndertakingFormPage(),
  sector: UndertakingSectorFormPage = UndertakingSectorFormPage(),
  verifiedEmail: UndertakingConfirmEmailFormPage = UndertakingConfirmEmailFormPage(),
  addBusiness: UndertakingAddBusinessFormPage = UndertakingAddBusinessFormPage(),
  cya: UndertakingCyaFormPage = UndertakingCyaFormPage(),
  confirmation: UndertakingConfirmationFormPage = UndertakingConfirmationFormPage(),
  isAmend: Boolean = false
) extends Journey {

  override def steps: Array[FormPage[_]] = Array(
    about,
    sector,
    verifiedEmail,
    addBusiness,
    cya,
    confirmation
  )
  private lazy val previousMap: Map[String, Uri] = Map(
    routes.UndertakingController.getAboutUndertaking.url -> routes.EligibilityController.getEoriCheck.url,
    routes.UndertakingController.getSector.url -> routes.UndertakingController.getAboutUndertaking.url
  )

  override def previous(implicit r: Request[_]): Uri =
    if (isAmend) routes.UndertakingController.getAmendUndertakingDetails.url
    else previousMap.getOrElse(r.uri, super.previous)

  override def next(implicit request: Request[_]): Future[Result] =
    if (isAmend) Redirect(routes.UndertakingController.getAmendUndertakingDetails).toFuture
    else if (requiredDetailsProvided) Redirect(routes.UndertakingController.getCheckAnswers).toFuture
    else super.next

  def isEmpty: Boolean = steps.flatMap(_.value).isEmpty

  private def requiredDetailsProvided =
    Seq(about, sector, verifiedEmail).map(_.value.isDefined) == Seq(true, true, true)

  def setUndertakingName(n: String): UndertakingJourney = this.copy(about = about.copy(value = Some(n)))

  def setUndertakingSector(s: Int): UndertakingJourney = this.copy(sector = sector.copy(value = Some(Sector(s))))

  def setUndertakingCYA(b: Boolean): UndertakingJourney = this.copy(cya = cya.copy(value = Some(b)))

  def setVerifiedEmail(e: String): UndertakingJourney = this.copy(verifiedEmail = verifiedEmail.copy(value = Some(e)))

  def setAddBusiness(b: Boolean): UndertakingJourney = this.copy(addBusiness = addBusiness.copy(value = b.some))

  def setUndertakingConfirmation(b: Boolean): UndertakingJourney =
    this.copy(confirmation = confirmation.copy(value = Some(b)))

}

object UndertakingJourney {

  implicit val format: Format[UndertakingJourney] = Json.format[UndertakingJourney]

  def fromUndertaking(undertaking: Undertaking): UndertakingJourney = UndertakingJourney(
    about = AboutUndertakingFormPage(undertaking.name.some),
    sector = UndertakingSectorFormPage(undertaking.industrySector.some)
  )

  object Forms {

    private val controller = routes.UndertakingController

    case class AboutUndertakingFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getAboutUndertaking.url
    }
    case class UndertakingSectorFormPage(value: Form[Sector] = None) extends FormPage[Sector] {
      def uri = controller.getSector.url
    }
    case class UndertakingConfirmEmailFormPage(value: Form[String] = None) extends FormPage[String] {
      def uri = controller.getConfirmEmail(isAmend = false).url
    }
    case class UndertakingAddBusinessFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getAddBusiness.url
    }
    case class UndertakingCyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCheckAnswers.url
    }
    case class UndertakingConfirmationFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.postConfirmation.url
    }

    object AboutUndertakingFormPage {
      implicit val undertakingNameFormPage: OFormat[AboutUndertakingFormPage] = Json.format
    }
    object UndertakingSectorFormPage {
      implicit val undertakingSectorFormPage: OFormat[UndertakingSectorFormPage] = Json.format
    }
    object UndertakingConfirmEmailFormPage {
      implicit val confirmEmailFormPageFormat: OFormat[UndertakingConfirmEmailFormPage] = Json.format
    }
    object UndertakingAddBusinessFormPage {
      implicit val confirmEmailFormPageFormat: OFormat[UndertakingAddBusinessFormPage] = Json.format
    }
    object UndertakingCyaFormPage { implicit val undertakingCyaFormPage: OFormat[UndertakingCyaFormPage] = Json.format }
    object UndertakingConfirmationFormPage {
      implicit val undertakingConfirmationFormPage: OFormat[UndertakingConfirmationFormPage] = Json.format
    }

  }
}
