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

import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.UndertakingAddBusinessPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class UndertakingAddBusinessController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override protected val store: Store,
  override protected val escService: EscService,
  undertakingAddBusinessPage: UndertakingAddBusinessPage
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  override val messagesApi: MessagesApi = mcc.messagesApi
  private val addBusinessForm: Form[FormValues] = formWithSingleMandatoryField("addBusinessIntent")

  protected def withJourneyOrRedirect[A : ClassTag](
    noJourneyFound: Call
  )(f: A => Future[Result])(implicit eori: EORI, reads: Reads[A]): Future[Result] =
    store
      .get[A]
      .toContext
      .foldF(Redirect(noJourneyFound).toFuture)(f)

  def getAddBusiness: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      runStepIfEligible(journey) {
        val form = journey.addBusiness.value.fold(addBusinessForm)(addBusiness =>
          addBusinessForm.fill(FormValues(addBusiness.toString))
        )
        Ok(
          undertakingAddBusinessPage(
            form,
            journey.previous
          )
        ).toFuture
      }
    }
  }

  def postAddBusiness: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(undertakingAddBusinessPage(errors, journey.previous)).toContext,
          form =>
            store
              .update[UndertakingJourney](_.setAddBusiness(form.value.isTrue))
              .flatMap(_.next)
              .toContext
        )
    }
  }

  def backFromCheckYourAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[UndertakingJourney](_.setUndertakingCYA(false))
      .map { _ =>
        Redirect(routes.BeneficiaryNotificationController.showPage())
      }
  }
}
