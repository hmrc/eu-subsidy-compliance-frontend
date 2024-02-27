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
class UndertakingCheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  override val emailService: EmailService,
  override val emailVerificationService: EmailVerificationService,
  val errorHandler: ErrorHandler,
  timeProvider: TimeProvider,
  auditService: AuditService,
  override val confirmEmailPage: ConfirmEmailPage,
  override val inputEmailPage: InputEmailPage,
  cyaPage: UndertakingCheckYourAnswersPage,
  undertakingCache: UndertakingCache
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseUndertakingController(
      mcc
    ) {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")

  def backFromCheckYourAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[UndertakingJourney](_.setUndertakingCYA(false))
      .map { _ =>
        Redirect(routes.UndertakingAddBusinessController.getAddBusiness)
      }
  }

  def getCheckAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.AboutUndertakingController.getAboutUndertaking).toFuture) { journey =>
        val result = for {
          undertakingSector <- journey.sector.value.toContext
          undertakingVerifiedEmail <- emailService.retrieveVerifiedEmailAddressByEORI(eori).toContext
          undertakingAddBusiness <- journey.addBusiness.value.toContext
          _ <- store.update[UndertakingJourney](j => j.copy(cya = UndertakingCyaFormPage(Some(true)))).toContext
        } yield Ok(
          cyaPage(
            eori = eori,
            sector = undertakingSector,
            verifiedEmail = undertakingVerifiedEmail,
            addBusiness = undertakingAddBusiness.toString,
            previous = routes.UndertakingCheckYourAnswersController.backFromCheckYourAnswers.url
          )
        )

        result.getOrElse(Redirect(journey.previous))
      }
  }

  def postCheckAnswers: Action[AnyContent] = verifiedEoriUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("Invalid form submission"),
        form => {
          val result = for {
            updatedJourney <- store.update[UndertakingJourney](_.setUndertakingCYA(form.value.toBoolean)).toContext
            undertakingName <- updatedJourney.about.value.toContext
            undertakingSector <- updatedJourney.sector.value.toContext
            undertaking = UndertakingCreate(
              name = UndertakingName(undertakingName),
              industrySector = undertakingSector,
              List(BusinessEntity(eori, leadEORI = true))
            )
            undertakingCreated <- createUndertakingAndSendEmail(undertaking).toContext
            _ <- store.update[UndertakingJourney](_.setSubmitted(true)).toContext
          } yield undertakingCreated
          result.fold(handleMissingSessionData("Undertaking create journey"))(identity)
        }
      )
  }

  private def createUndertakingAndSendEmail(
    undertaking: UndertakingCreate
  )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] =
    (
      for {
        ref <- escService.createUndertaking(undertaking)
        newlyCreatedUndertaking <- escService.retrieveUndertaking(eori).map {
          case Some(ut) => ut
          case None =>
            logger.error(s"Unable to fetch undertaking with reference: $ref")
            throw new NotFoundException(s"Unable to fetch undertaking with reference: $ref")
        }
        _ <- emailService.sendEmail(eori, EmailTemplate.CreateUndertaking, newlyCreatedUndertaking)
        _ <- undertakingCache.put[Undertaking](eori, newlyCreatedUndertaking)
        auditEventCreateUndertaking = AuditEvent.CreateUndertaking(
          request.authorityId,
          ref,
          undertaking,
          sectorCap = newlyCreatedUndertaking.industrySectorLimit,
          timeProvider.now
        )
        _ = auditService.sendEvent[CreateUndertaking](auditEventCreateUndertaking)
      } yield Redirect(routes.UndertakingConfirmationController.getConfirmation(ref))
    ).recover { case error: NotFoundException =>
      logger.error(s"Error creating undertaking: $error")

      InternalServerError(
        errorHandler.internalServerErrorTemplate
      )
    }
}
