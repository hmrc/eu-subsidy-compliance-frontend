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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.data.OptionT
import cats.implicits.catsSyntaxOptionId
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField, mandatory}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityRemovedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.formatEori
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessEntityEoriController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  timeProvider: TimeProvider,
  emailService: EmailService,
  auditService: AuditService,
  eoriPage: BusinessEntityEoriPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (EORI.ValidLengthsWithPrefix.contains(eori.replaceAll(" ", "").length)) Valid
    else Invalid("businessEntityEori.error.incorrect-length")
  }

  private val isEoriValid = Constraint[String] { eori: String =>
    if (appConfig.xiEoriAddingDisabled && eori.replaceAll(" ", "").matches("""^(gb|Gb|gB|GB)[0-9]{12,15}$""")) Valid
    else if (!appConfig.xiEoriAddingDisabled && eori.replaceAll(" ", "").matches(EORI.regex)) Valid
    else Invalid("businessEntityEori.regex.error")
  }

  private val eoriForm: Form[FormValues] = Form(
    mapping(
      "businessEntityEori" -> mandatory("businessEntityEori")
        .verifying(isEoriLengthValid)
        .verifying(isEoriValid)
    )(eoriEntered => FormValues(eoriEntered))(eori => eori.value.some)
  )

  def getEori: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("AddBusinessEntityController.getEori")
      store
        .get[BusinessEntityJourney]
        .toContext
        .foldF(Redirect(routes.AddBusinessEntityController.getAddBusinessEntity()).toFuture) { journey =>
          runStepIfEligible(journey) {
            val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori)))
            Ok(eoriPage(form, journey.previous)).toFuture
          }
        }
    }
  }

  def postEori: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("AddBusinessEntityController.postEori")

    val businessEntityEori = "businessEntityEori"

    def createJourneyAndRedirect(businessEntityJourney: BusinessEntityJourney)(implicit eori: EORI) =
      if (businessEntityJourney.onLeadSelectJourney)
        store
          .put[BusinessEntityJourney](BusinessEntityJourney(isLeadSelectJourney = true.some))
          .map(_ => Redirect(routes.SelectNewLeadController.getSelectNewLead))
      else Future.successful(Redirect(routes.AddBusinessEntityController.startJourney(businessAdded = Some(true))))

    def getErrorResponse(errorMessageKey: String, previous: String, form: FormValues): Future[Result] =
      BadRequest(eoriPage(eoriForm.withError(businessEntityEori, errorMessageKey).fill(form), previous)).toFuture

    def updateOrCreateBusinessEntity(journey: BusinessEntityJourney, ref: UndertakingRef, eori: EORI) = {
      val businessEntity = BusinessEntity(eori, leadEORI = false)
      if (journey.isAmend)
        escService.removeMember(ref, businessEntity.copy(businessEntityIdentifier = journey.oldEORI.get))
      else escService.addMember(ref, businessEntity).toContext
    }

    def sendAuditEvent(journey: BusinessEntityJourney, ref: UndertakingRef, businessEori: EORI) =
      if (journey.isAmend)
        auditService.sendEvent(AuditEvent.BusinessEntityUpdated(ref, request.authorityId, eori, businessEori))
      else auditService.sendEvent(AuditEvent.BusinessEntityAdded(ref, request.authorityId, eori, businessEori))

    def handleValidEori(form: FormValues, previous: Uri, undertaking: Undertaking): Future[Result] = {
      val businessEori = EORI(formatEori(form.value))
      escService.retrieveUndertakingAndHandleErrors(businessEori).flatMap {
        case Right(Some(_)) => getErrorResponse("businessEntityEori.eoriInUse", previous, form)
        case Left(_) => getErrorResponse(s"error.$businessEntityEori.required", previous, form)
        case Right(None) =>
          val result = for {
            businessEntityJourney <- store.get[BusinessEntityJourney].toContext
            undertakingRef <- undertaking.reference.toContext
            _ <- updateOrCreateBusinessEntity(businessEntityJourney, undertakingRef, businessEori).toContext
            _ <- emailService.sendEmail(businessEori, AddMemberToBusinessEntity, undertaking).toContext
            _ <- emailService.sendEmail(eori, businessEori, AddMemberToLead, undertaking).toContext
            _ = sendAuditEvent(businessEntityJourney, undertakingRef, businessEori)
            redirect <- createJourneyAndRedirect(businessEntityJourney)(eori).toContext
          } yield redirect
          result.fold(handleMissingSessionData("BusinessEntity Data"))(identity)
      }
    }

    withLeadUndertaking { undertaking =>
      store
        .get[BusinessEntityJourney]
        .toContext
        .foldF(Redirect(routes.AddBusinessEntityController.getAddBusinessEntity()).toFuture) { journey =>
          runStepIfEligible(journey) {
            eoriForm
              .bindFromRequest()
              .fold(
                errors => {
                  logger.info("AddBusinessEntityController.postEori failed validation, showing errors")
                  BadRequest(eoriPage(errors, journey.previous)).toFuture
                },
                form => {
                  logger.info("AddBusinessEntityController.postEori succeeded validation")
                  handleValidEori(form, journey.previous, undertaking)
                }
              )
          }
        }
    }
  }
}
