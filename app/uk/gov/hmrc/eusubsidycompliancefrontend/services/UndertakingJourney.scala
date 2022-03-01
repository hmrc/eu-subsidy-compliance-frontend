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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.{Form, Uri}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.UndertakingJourney.Forms.{UndertakingConfirmationFormPage, UndertakingCyaFormPage, UndertakingNameFormPage, UndertakingSectorFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

case class UndertakingJourney(
  name: UndertakingNameFormPage = UndertakingNameFormPage(),
  sector: UndertakingSectorFormPage = UndertakingSectorFormPage(),
  cya: UndertakingCyaFormPage = UndertakingCyaFormPage(),
  confirmation: UndertakingConfirmationFormPage = UndertakingConfirmationFormPage(),
  isAmend: Boolean = false
) extends Journey {

  override protected def steps = List(
    name,
    sector,
    cya,
    confirmation
  )

  override def previous(implicit request: Request[_]): Uri =
    if (isAmend) routes.UndertakingController.getAmendUndertakingDetails().url
    else if (requiredDetailsProvided) routes.UndertakingController.getCheckAnswers().url
    else super.previous

  override def next(implicit request: Request[_]): Future[Result] =
    if (isAmend) Redirect(routes.UndertakingController.getAmendUndertakingDetails()).toFuture
    else if (requiredDetailsProvided) Redirect(routes.UndertakingController.getCheckAnswers()).toFuture
    else super.next

  private def requiredDetailsProvided =
    Seq(name, sector).map(_.value.isDefined) == Seq(true, true)

}

object UndertakingJourney {

  implicit val format: Format[UndertakingJourney] = Json.format[UndertakingJourney]

  // TODO - review how / where this is used
  def fromUndertakingOpt(undertakingOpt: Option[Undertaking]): UndertakingJourney = undertakingOpt match {
    case Some(undertaking) =>
      val empty = UndertakingJourney()
      empty.copy(
        name = empty.name.copy(value = undertaking.name.some),
        sector = empty.sector.copy(value = undertaking.industrySector.some),
        isAmend = false
      )

    case _ => UndertakingJourney()
  }

  object FormUrls {
    val Name = "undertaking-name"
    val Sector = "sector"
    val Cya = "check-your-answers"
    val Confirmation = "confirmation"
  }

  object Forms {
    // TODO - replace uris with routes lookups
    case class UndertakingNameFormPage(value: Form[String] = None) extends FormPage[String] { val uri = FormUrls.Name }
    case class UndertakingSectorFormPage(value: Form[Sector] = None) extends FormPage[Sector] { val uri = FormUrls.Sector }
    case class UndertakingCyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { val uri = FormUrls.Cya }
    case class UndertakingConfirmationFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { val uri = FormUrls.Confirmation }

    object UndertakingNameFormPage { implicit val undertakingNameFormPage: OFormat[UndertakingNameFormPage] = Json.format }
    object UndertakingSectorFormPage { implicit val undertakingSectorFormPage: OFormat[UndertakingSectorFormPage] = Json.format }
    object UndertakingCyaFormPage { implicit val undertakingCyaFormPage: OFormat[UndertakingCyaFormPage] = Json.format }
    object UndertakingConfirmationFormPage { implicit val undertakingConfirmationFormPage: OFormat[UndertakingConfirmationFormPage] = Json.format }
  }
}
