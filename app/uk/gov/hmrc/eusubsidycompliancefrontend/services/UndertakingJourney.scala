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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.UndertakingJourney.FormUrls
import uk.gov.hmrc.eusubsidycompliancefrontend.util.FutureSyntax.FutureOps

import scala.concurrent.Future

case class UndertakingJourney(
  name: FormPage[String] = FormPage(FormUrls.Name),
  sector: FormPage[Sector] = FormPage(FormUrls.Sector),
  cya: FormPage[Boolean] = FormPage(FormUrls.Cya),
  confirmation: FormPage[Boolean] = FormPage(FormUrls.Confirmation),
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
  import Journey._

  implicit val formPageSectorFormat: OFormat[FormPage[Sector]] = Json.format[FormPage[Sector]]

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
    val Name ="undertaking-name"
    val Sector = "sector"
    val Contact = "contact"
    val Cya = "check-your-answers"
    val Confirmation = "confirmation"
  }

}