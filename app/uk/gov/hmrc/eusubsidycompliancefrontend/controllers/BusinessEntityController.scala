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
class BusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  timeProvider: TimeProvider,
  emailService: EmailService,
  auditService: AuditService,
  addBusinessPage: AddBusinessPage,
  eoriPage: BusinessEntityEoriPage,
  removeYourselfBEPage: BusinessEntityRemoveYourselfPage,
  removeBusinessPage: RemoveBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val addBusinessForm = formWithSingleMandatoryField("addBusiness")
  private val removeBusinessForm = formWithSingleMandatoryField("removeBusiness")
  private val removeYourselfBusinessForm = formWithSingleMandatoryField("removeYourselfBusinessEntity")

  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (EORI.ValidLengthsWithPrefix.contains(eori.replaceAll(" ", "").length)) Valid
    else Invalid("businessEntityEori.error.incorrect-length")
  }

  private val isEoriValid = Constraint[String] { eori: String =>
    if (eori.replaceAll(" ", "").matches(EORI.regex)) Valid
    else Invalid("businessEntityEori.regex.error")
  }

  private val eoriForm: Form[FormValues] = Form(
    mapping(
      "businessEntityEori" -> mandatory("businessEntityEori")
        .verifying(isEoriLengthValid)
        .verifying(isEoriValid)
    )(eoriEntered => FormValues(eoriEntered))(eori => eori.value.some)
  )

  def startJourney(businessAdded: Option[Boolean] = None, businessRemoved: Option[Boolean] = None): Action[AnyContent] =
    verifiedEmail.async { implicit request =>
      withLeadUndertaking { _ =>
        startNewJourney { _ =>
          Redirect(routes.BusinessEntityController.getAddBusinessEntity(businessAdded, businessRemoved).url)
        }
      }
    }

  def getAddBusinessEntity(
    businessAdded: Option[Boolean] = None,
    businessRemoved: Option[Boolean] = None
  ): Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("BusinessEntityController.getAddBusinessEntity")
      store
        .getOrCreate[BusinessEntityJourney](BusinessEntityJourney())
        .map { journey =>
          val form =
            journey.addBusiness.value
              .fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))

          logger.info("BusinessEntityController showing undertakingBusinessEntity")
          Ok(
            addBusinessPage(
              form,
              undertaking.undertakingBusinessEntity,
              businessAdded.getOrElse(false),
              businessRemoved.getOrElse(false)
            )
          )
        }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("BusinessEntityController.postAddBusinessEntity")
    def handleValidAnswer(form: FormValues) =
      if (form.value.isTrue) store.update[BusinessEntityJourney](_.setAddBusiness(form.value.toBoolean)).flatMap(_.next)
      else Redirect(routes.AccountController.getAccountPage).toFuture

    withLeadUndertaking { undertaking =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(addBusinessPage(errors, undertaking.undertakingBusinessEntity)).toFuture,
          handleValidAnswer
        )
    }
  }

  def getEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("BusinessEntityController.getEori")
      store
        .get[BusinessEntityJourney]
        .toContext
        .foldF(Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture) { journey =>
          runStepIfEligible(journey) {
            val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori)))
            Ok(eoriPage(form, journey.previous)).toFuture
          }
        }
    }
  }

  def postEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("BusinessEntityController.postEori")

    val businessEntityEori = "businessEntityEori"

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
        .foldF(Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture) { journey =>
          runStepIfEligible(journey) {
            eoriForm
              .bindFromRequest()
              .fold(
                errors => {
                  logger.info("BusinessEntityController.postEori failed validation, showing errors")
                  BadRequest(eoriPage(errors, journey.previous)).toFuture
                },
                form => {
                  logger.info("BusinessEntityController.postEori succeeded validation")
                  handleValidEori(form, journey.previous, undertaking)
                }
              )
          }
        }
    }
  }

  protected def startNewJourney(
    f: BusinessEntityJourney => Result,
    businessAdded: Option[Boolean] = None,
    businessRemoved: Option[Boolean] = None
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber
    store
      .put[BusinessEntityJourney](BusinessEntityJourney())
      .toContext
      .map(journey => f(journey))
      .getOrElse(Redirect(routes.BusinessEntityController.getAddBusinessEntity(businessAdded, businessRemoved).url))

  }

  private def createJourneyAndRedirect(businessEntityJourney: BusinessEntityJourney)(implicit EORI: EORI) =
    if (businessEntityJourney.onLeadSelectJourney)
      store
        .put[BusinessEntityJourney](BusinessEntityJourney(isLeadSelectJourney = true.some))
        .map(_ => Redirect(routes.SelectNewLeadController.getSelectNewLead))
    else Future.successful(Redirect(routes.BusinessEntityController.startJourney(businessAdded = Some(true))))

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async { implicit request =>
    logger.info("BusinessEntityController.getRemoveBusinessEntity")
    withLeadUndertaking { _ =>
      escService.retrieveUndertaking(EORI(eoriEntered)).map {
        case Some(undertaking) =>
          logger.info(s"Found undertaking for $eoriEntered")
          val removeBE = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))
          Ok(removeBusinessPage(removeBusinessForm, removeBE))
        case _ =>
          logger.info(
            s"Did not find undertaking for $eoriEntered, redirecting to BusinessEntityController.getAddBusinessEntity"
          )
          Redirect(routes.BusinessEntityController.getAddBusinessEntity())
      }
    }
  }

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("BusinessEntityController.getRemoveYourselfBusinessEntity")

    val previous = routes.AccountController.getAccountPage.url

    escService
      .retrieveUndertaking(eori)
      .toContext
      .fold {
        logger.info(
          s"Could not find undertaking for $eori, redirecting to BusinessEntityController.getAddBusinessEntity"
        )
        Redirect(routes.BusinessEntityController.getAddBusinessEntity())
      } { undertaking =>
        logger.info(
          s"Found undertaking for $eori, showing removeYourselfBEPage"
        )
        Ok(removeYourselfBEPage(removeYourselfBusinessForm, undertaking.getBusinessEntityByEORI(eori), previous))
      }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async {
    implicit request: AuthenticatedEnrolledRequest[AnyContent] =>
      logger.info("BusinessEntityController.postRemoveBusinessEntity")

      withLeadUndertaking { _ =>
        escService
          .retrieveUndertaking(EORI(eoriEntered))
          .toContext
          .foldF(Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture) { undertaking =>
            val undertakingRef = undertaking.reference
            val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))

            removeBusinessForm
              .bindFromRequest()
              .fold(
                errors => BadRequest(removeBusinessPage(errors, removeBE)).toFuture,
                success = form => handleValidBE(eoriEntered, form, undertakingRef, removeBE, undertaking)
              )
          }
      }
  }

  private def handleValidBE(
    eoriEntered: String,
    form: FormValues,
    undertakingRef: UndertakingRef,
    businessEntityToRemove: BusinessEntity,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    implicit val eori: EORI = request.eoriNumber
    logger.info(s"handleValidBE for eoriEntered:$eoriEntered")

    if (form.value.isTrue) {
      logger.info("processing form as true is selected")
      val effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
      for {
        _ <- escService.removeMember(undertakingRef, businessEntityToRemove)
        _ <- emailService.sendEmail(EORI(eoriEntered), RemoveMemberToBusinessEntity, undertaking, effectiveDate)
        _ <- emailService.sendEmail(eori, EORI(eoriEntered), RemoveMemberToLead, undertaking, effectiveDate)
        _ = auditService
          .sendEvent(
            AuditEvent.BusinessEntityRemoved(undertakingRef, request.authorityId, eori, EORI(eoriEntered))
          )
      } yield {
        logger.info(
          s"Removed undertakingRef:$undertakingRef, Redirecting to BusinessEntityController.getAddBusinessEntity"
        )
        Redirect(routes.BusinessEntityController.startJourney(businessRemoved = Some(true)))
      }
    } else {
      //fixme why do we need this?
      logger.info(
        s"Did not remove undertakingRef:$undertakingRef as form was not true, redirecting to BusinessEntityController.getAddBusinessEntity"
      )
      Redirect(routes.BusinessEntityController.startJourney()).toFuture
    }
  }

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("BusinessEntityController.postRemoveYourselfBusinessEntity")
    def handleFormSubmission(undertaking: Undertaking, businessEntity: BusinessEntity): OptionT[Future, Result] =
      removeYourselfBusinessForm
        .bindFromRequest()
        .fold(
          errors => {
            logger.info("Failed validating postRemoveYourselfBusinessEntity, showing errors")
            BadRequest(
              removeYourselfBEPage(errors, businessEntity, routes.AccountController.getAccountPage.url)
            ).toContext
          }, {
            logger
              .info(s"Passed validating postRemoveYourselfBusinessEntity, handleValidFormSubmission for $undertaking")
            handleValidFormSubmission(undertaking, businessEntity)
          }
        )

    def handleValidFormSubmission(undertaking: Undertaking, businessEntity: BusinessEntity)(
      form: FormValues
    ): OptionT[Future, Result] =
      if (form.value.isTrue) {
        for {
          _ <- escService.removeMember(undertaking.reference, businessEntity).toContext
          _ <- store.deleteAll.toContext
          effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
          _ <- emailService.sendEmail(eori, MemberRemoveSelfToBusinessEntity, undertaking, effectiveDate).toContext
          _ <- emailService
            .sendEmail(undertaking.getLeadEORI, eori, MemberRemoveSelfToLead, undertaking, effectiveDate)
            .toContext
          _ = auditService.sendEvent(
            BusinessEntityRemovedSelf(undertaking.reference, request.authorityId, undertaking.getLeadEORI, eori)
          )
        } yield Redirect(routes.SignOutController.signOut())
      } else Redirect(routes.AccountController.getAccountPage).toContext

    val result = for {
      undertaking <- escService.retrieveUndertaking(eori).toContext
      businessEntityToRemove <- undertaking.findBusinessEntity(eori).toContext
      r <- handleFormSubmission(undertaking, businessEntityToRemove)
    } yield r

    result.getOrElse(handleMissingSessionData("Undertaking"))
  }

}
