/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.data.OptionT
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmationPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingConfirmationController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  override protected val escService: EscService,
  confirmationPage: ConfirmationPage
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi

  private val confirmationForm: Form[FormValues] =
    formWithSingleMandatoryField("confirm")

  def getConfirmation(ref: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        val result: OptionT[Future, Result] = for {
          addBusiness <- journey.addBusiness.value.toContext
        } yield Ok(confirmationPage(UndertakingRef(ref), eori, addBusiness))
        result.fold(Redirect(journey.previous))(identity)
      case _ => Redirect(routes.UndertakingController.getAboutUndertaking).toFuture
    }
  }

  def postConfirmation: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    confirmationForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("Unexpected form submission"),
        form =>
          store
            .update[UndertakingJourney](_.setUndertakingConfirmation(form.value.toBoolean))
            .map { _ =>
              Redirect(routes.AccountController.getAccountPage)
            }
      )
  }
}
