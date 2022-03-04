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

  override def steps = Array(
    name,
    sector,
    cya,
    confirmation
  )

  override def previous(implicit r: Request[_]): Uri =
    if (isAmend) routes.UndertakingController.getAmendUndertakingDetails().url
    else if (requiredDetailsProvided) routes.UndertakingController.getCheckAnswers().url
    else super.previous

  override def next(implicit request: Request[_]): Future[Result] =
    if (isAmend) Redirect(routes.UndertakingController.getAmendUndertakingDetails()).toFuture
    else if (requiredDetailsProvided) Redirect(routes.UndertakingController.getCheckAnswers()).toFuture
    else super.next

  // Returns a new UndertakingJourney without user entered values except name and sector which cannot be changed.
  // TODO - is this needed?
  def clearUserData: UndertakingJourney = UndertakingJourney(
    name = name,
    sector = sector,
  )

  // TODO - could call this not started
  def isEmpty: Boolean = steps.flatMap(_.value).isEmpty

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

  def fromUndertaking(undertaking: Undertaking): UndertakingJourney = UndertakingJourney(
    name = UndertakingNameFormPage(undertaking.name.some),
    sector = UndertakingSectorFormPage(undertaking.industrySector.some)
  )

  object Forms {

    private val controller = routes.UndertakingController

    case class UndertakingNameFormPage(value: Form[String] = None) extends FormPage[String] { def uri = controller.getUndertakingName().url }
    case class UndertakingSectorFormPage(value: Form[Sector] = None) extends FormPage[Sector] { def uri = controller.getSector().url }
    case class UndertakingCyaFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { def uri = controller.getCheckAnswers().url }
    case class UndertakingConfirmationFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] { def uri = controller.postConfirmation().url }

    object UndertakingNameFormPage { implicit val undertakingNameFormPage: OFormat[UndertakingNameFormPage] = Json.format }
    object UndertakingSectorFormPage { implicit val undertakingSectorFormPage: OFormat[UndertakingSectorFormPage] = Json.format }
    object UndertakingCyaFormPage { implicit val undertakingCyaFormPage: OFormat[UndertakingCyaFormPage] = Json.format }
    object UndertakingConfirmationFormPage { implicit val undertakingConfirmationFormPage: OFormat[UndertakingConfirmationFormPage] = Json.format }

  }
}
