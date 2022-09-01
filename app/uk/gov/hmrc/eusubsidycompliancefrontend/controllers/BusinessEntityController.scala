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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{AddMemberToBusinessEntity, AddMemberToLead, RemoveMemberToBusinessEntity, RemoveMemberToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.getValidEori
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

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
                                           businessEntityCyaPage: BusinessEntityCYAPage,
                                           removeBusinessPage: RemoveBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with FormHelpers {

  import actionBuilders._

  def getAddBusinessEntity: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      store
        .getOrCreate[BusinessEntityJourney](BusinessEntityJourney())
        .map { journey =>
          val form = journey.addBusiness.value
            .fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))

          Ok(
            addBusinessPage(
              form,
              undertaking.name,
              undertaking.undertakingBusinessEntity
            )
          )
        }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswer(form: FormValues) =
      if (form.value.isTrue)
        store.update[BusinessEntityJourney](_.setAddBusiness(form.value.toBoolean)).flatMap(_.next)
      else Redirect(routes.AccountController.getAccountPage()).toFuture

    withLeadUndertaking { undertaking =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(addBusinessPage(errors, undertaking.name, undertaking.undertakingBusinessEntity)).toFuture,
          handleValidAnswer
        )
    }
  }

  def getEori: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store.get[BusinessEntityJourney].flatMap {
        case Some(journey) =>
          if (!journey.isEligibleForStep) {
            Redirect(journey.previous).toFuture
          } else {
            val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori)))
            Ok(eoriPage(form, journey.previous)).toFuture
          }
        case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
      }

    }
  }

  def postEori: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val businessEntityEori = "businessEntityEori"

    def getErrorResponse(errorMessageKey: String, previous: String, form: FormValues): Future[Result] =
      BadRequest(eoriPage(eoriForm.withError(businessEntityEori, errorMessageKey).fill(form), previous)).toFuture

    def handleValidEori(form: FormValues, previous: Uri): Future[Result] =
      escService.retrieveUndertakingAndHandleErrors(EORI(form.value)).flatMap {
        case Right(Some(_)) => getErrorResponse("businessEntityEori.eoriInUse", previous, form)
        case Left(_) => getErrorResponse(s"error.$businessEntityEori.required", previous, form)
        case Right(None) => store.update[BusinessEntityJourney](_.setEori(EORI(form.value))).flatMap(_.next)
      }

    withLeadUndertaking { _ =>
      processFormSubmission[BusinessEntityJourney] { journey =>
        eoriForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(eoriPage(errors, journey.previous)).toContext,
            form => handleValidEori(form, journey.previous).toContext
          )
      }
    }
  }

  def getCheckYourAnswers: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    withLeadUndertaking { _ =>
      store.get[BusinessEntityJourney].flatMap {
        case Some(journey) =>
          if (!journey.isEligibleForStep) {
            Redirect(journey.previous).toFuture
          } else {
            Ok(businessEntityCyaPage(journey.eori.value.get, journey.previous)).toFuture
          }
        case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
      }
    }
  }

  def postCheckYourAnswers: Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswersC(undertaking: Undertaking) = for {
      businessEntityJourney <- store.get[BusinessEntityJourney].toContext
      undertakingRef <- undertaking.reference.toContext
      eoriBE <- businessEntityJourney.eori.value.toContext
      businessEntity = BusinessEntity(eoriBE, leadEORI = false) // resetting the journey as it's final CYA page
      _ <- {
        if (businessEntityJourney.isAmend) {
          escService
            .removeMember(
              undertakingRef,
              businessEntity.copy(businessEntityIdentifier = businessEntityJourney.oldEORI.get)
            )
            .toContext
        }
        escService.addMember(undertakingRef, businessEntity).toContext
      }
      _ <- emailService.sendEmail(eoriBE, AddMemberToBusinessEntity, undertaking).toContext
      _ <- emailService.sendEmail(eori, eoriBE, AddMemberToLead, undertaking).toContext
      // Clear the cached undertaking so it's retrieved on the next access
      _ <- store.delete[Undertaking].toContext
      _ =
        if (businessEntityJourney.isAmend)
          auditService.sendEvent(
            AuditEvent.BusinessEntityUpdated(undertakingRef, request.authorityId, eori, eoriBE)
          )
        else
          auditService.sendEvent(AuditEvent.BusinessEntityAdded(undertakingRef, request.authorityId, eori, eoriBE))
      redirect <- getNext(businessEntityJourney)(eori).toContext
    } yield redirect

    withLeadUndertaking { undertaking =>
      cyaForm
        .bindFromRequest()
        .fold(
          errors => throw new IllegalStateException(s"Error processing BusinessEntity CYA form: $errors"),
          _ => handleValidAnswersC(undertaking).fold(handleMissingSessionData("BusinessEntity Data"))(identity)
        )
    }
  }

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async {
    implicit request =>
      withLeadUndertaking { _ =>
        escService.retrieveUndertaking(EORI(eoriEntered)).map {
          case Some(undertaking) =>
            val removeBE = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))
            Ok(removeBusinessPage(removeBusinessForm, removeBE))
          case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
        }
      }
  }

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield undertakingOpt match {
      case Some(undertaking) =>
        val removeBE = undertaking.getBusinessEntityByEORI(eori)
        Ok(removeYourselfBEPage(removeYourselfBusinessForm, removeBE, previous, undertaking.name))

      case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidBE(
        form: FormValues,
        undertakingRef: UndertakingRef,
        removeBE: BusinessEntity,
        undertaking: Undertaking
      ) =
        if (form.value.isTrue) {
            val removalEffectiveDateString = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
            for {
              _ <- escService.removeMember(undertakingRef, removeBE)
              _ <- emailService.sendEmail(
                EORI(eoriEntered),
                RemoveMemberToBusinessEntity,
                undertaking,
                removalEffectiveDateString
              )
              _ <- emailService.sendEmail(
                eori,
                EORI(eoriEntered),
                RemoveMemberToLead,
                undertaking,
                removalEffectiveDateString
              )
              // Clear the cached undertaking so it's retrieved on the next access
              _ <- store.delete[Undertaking]
              _ = auditService
                .sendEvent(
                  AuditEvent.BusinessEntityRemoved(undertakingRef, request.authorityId, eori, EORI(eoriEntered))
                )
            } yield Redirect(routes.BusinessEntityController.getAddBusinessEntity())
        }
        else Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture

      withLeadUndertaking { _ =>
        escService.retrieveUndertaking(EORI(eoriEntered)).flatMap {
          case Some(undertaking) =>
            val undertakingRef = undertaking.reference
            val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))

            removeBusinessForm
              .bindFromRequest()
              .fold(
                errors => BadRequest(removeBusinessPage(errors, removeBE)).toFuture,
                success = form => handleValidBE(form, undertakingRef, removeBE, undertaking)
              )
          case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
        }
      }
  }

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    val loggedInEORI = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    escService.retrieveUndertaking(loggedInEORI).flatMap {
      case Some(undertaking) => {
        val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(loggedInEORI)
        def handleValidBE(form: FormValues) =
          if (form.value.isTrue) Redirect(routes.SignOutController.signOut()).toFuture
          else Redirect(routes.AccountController.getAccountPage()).toFuture
        removeYourselfBusinessForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(removeYourselfBEPage(errors, removeBE, previous, undertaking.name)).toFuture,
            form => handleValidBE(form)
          )
      }
      case None => handleMissingSessionData("Undertaking journey")
    }
  }

  private def getNext(businessEntityJourney: BusinessEntityJourney)(implicit EORI: EORI): Future[Result] =
    businessEntityJourney.isLeadSelectJourney match {
      case Some(true) =>
        store
          .put[BusinessEntityJourney](BusinessEntityJourney(isLeadSelectJourney = true.some))
          .map(_ => Redirect(routes.SelectNewLeadController.getSelectNewLead()))
      case _ =>
        store
          .put[BusinessEntityJourney](BusinessEntityJourney())
          .map(_ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()))
    }

  private val addBusinessForm = formWithSingleMandatoryField("addBusiness")
  private val removeBusinessForm = formWithSingleMandatoryField("removeBusiness")
  private val removeYourselfBusinessForm = formWithSingleMandatoryField("removeYourselfBusinessEntity")

  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (getValidEori(eori).length === 14 || getValidEori(eori).length === 17) Valid
    else Invalid("businessEntityEori.error.incorrect-length")
  }

  private val isEoriValid = Constraint[String] { eori: String =>
    if (getValidEori(eori).matches(EORI.regex)) Valid
    else Invalid("businessEntityEori.regex.error")
  }

  private val eoriForm: Form[FormValues] = Form(
    mapping(
      "businessEntityEori" -> mandatory("businessEntityEori")
        .verifying(isEoriLengthValid)
        .verifying(isEoriValid)
    )(eoriEntered => FormValues(getValidEori(eoriEntered)))(eori => eori.value.drop(2).some)
  )

  private val cyaForm: Form[FormValues] = Form(mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

}
