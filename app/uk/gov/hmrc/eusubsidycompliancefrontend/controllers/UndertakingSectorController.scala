/*
 * Copyright 2024 HM Revenue & Customs
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
import cats.implicits._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField, mandatory}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EmailStatus, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{Store, UndertakingCache}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.{HeaderCarrier, HttpVerbs, NotFoundException}
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms.UndertakingCyaFormPage

@Singleton
class UndertakingSectorController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  override val emailService: EmailService,
  override val emailVerificationService: EmailVerificationService,
  undertakingSectorPage: UndertakingSectorPage,
  override val confirmEmailPage: ConfirmEmailPage,
  override val inputEmailPage: InputEmailPage
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseUndertakingController(
      mcc
    ) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val undertakingSectorForm: Form[FormValues] = formWithSingleMandatoryField("undertakingSector")

  def getSector: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    getSectorPage()
  }

  private def getSectorPage(isUpdate: Boolean = false)(implicit request: AuthenticatedEnrolledRequest[_]) = {
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.AboutUndertakingController.getAboutUndertaking) { journey =>
      runStepIfEligible(journey) {
        val form = journey.sector.value.fold(undertakingSectorForm) { sector =>
          undertakingSectorForm.fill(FormValues(sector.id.toString))
        }

        Ok(
          undertakingSectorPage(
            form,
            journey.previous,
            journey.about.value.getOrElse(""),
            isUpdate
          )
        ).toFuture
      }
    }
  }

  def getSectorForUpdate: Action[AnyContent] = enrolled.async { implicit request =>
    getSectorPage(isUpdate = true)
  }

  def postSector: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    updateSector()
  }
  def updateIndustrySector: Action[AnyContent] = enrolled.async { implicit request =>
    updateSector(isUpdate = true)
  }

  private def updateSector(isUpdate: Boolean = false)(implicit request: AuthenticatedEnrolledRequest[_]) = {
    implicit val eori: EORI = request.eoriNumber
    processFormSubmission[UndertakingJourney] { journey =>
      undertakingSectorForm
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(undertakingSectorPage(errors, journey.previous, journey.about.value.get, isUpdate)).toContext,
          form =>
            store
              .update[UndertakingJourney](_.setUndertakingSector(form.value.toInt))
              .flatMap(_.next)
              .toContext
        )
    }
  }
}
