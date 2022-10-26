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

import cats.implicits.catsSyntaxOptionId
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityRemovedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.withGbPrefix
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
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
                                           removeBusinessPage: RemoveBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with FormHelpers {

  import actionBuilders._

  def getAddBusinessEntity: Action[AnyContent] = verifiedEmail.async { implicit request =>
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
              undertaking.undertakingBusinessEntity
            )
          )
        }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = verifiedEmail.async { implicit request =>
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
            BadRequest(addBusinessPage(errors, undertaking.undertakingBusinessEntity)).toFuture,
          handleValidAnswer
        )
    }
  }

  def getEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store
        .get[BusinessEntityJourney]
        .toContext
        .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity())) { journey =>
          if (!journey.isEligibleForStep) Redirect(journey.previous)
          else {
            val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori)))
            Ok(eoriPage(form, journey.previous))
          }
        }
    }
  }

  def postEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val businessEntityEori = "businessEntityEori"

    def getErrorResponse(errorMessageKey: String, previous: String, form: FormValues): Future[Result] =
      BadRequest(eoriPage(eoriForm.withError(businessEntityEori, errorMessageKey).fill(form), previous)).toFuture

    def handleValidEori(form: FormValues, previous: Uri, undertaking: Undertaking): Future[Result] =
      escService.retrieveUndertakingAndHandleErrors(EORI(form.value)).flatMap {
        case Right(Some(_)) => getErrorResponse("businessEntityEori.eoriInUse", previous, form)
        case Left(_) => getErrorResponse(s"error.$businessEntityEori.required", previous, form)
        case Right(None) => {
          val result = for {
            businessEntityJourney <- store.get[BusinessEntityJourney].toContext
            undertakingRef <- undertaking.reference.toContext
            eoriBE = EORI(form.value)
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
            _ =
              if (businessEntityJourney.isAmend)
                auditService.sendEvent(
                  AuditEvent.BusinessEntityUpdated(undertakingRef, request.authorityId, eori, eoriBE)
                )
              else
                auditService.sendEvent(AuditEvent.BusinessEntityAdded(undertakingRef, request.authorityId, eori, eoriBE))
            redirect <- getNext(businessEntityJourney)(eori).toContext
          } yield redirect
          result.fold(handleMissingSessionData("BusinessEntity Data"))(identity)
        }
      }

    withLeadUndertaking { undertaking =>
      store
        .get[BusinessEntityJourney]
        .toContext
        .foldF(Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture) { journey =>
          if (!journey.isEligibleForStep) Redirect(journey.previous).toFuture
          else {
            eoriForm
              .bindFromRequest()
              .fold(
                errors => BadRequest(eoriPage(errors, journey.previous)).toFuture,
                form => handleValidEori(form, journey.previous, undertaking)
              )
          }
        }
    }
  }

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async {
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

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val previous = routes.AccountController.getAccountPage().url

    escService
      .retrieveUndertaking(eori)
      .toContext
      .fold(Redirect(routes.BusinessEntityController.getAddBusinessEntity())) { undertaking =>
        Ok(removeYourselfBEPage(removeYourselfBusinessForm, undertaking.getBusinessEntityByEORI(eori), previous))
      }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEmail.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidBE(form: FormValues, undertakingRef: UndertakingRef, removeBE: BusinessEntity, undertaking: Undertaking) =
        if (form.value.isTrue) {
            val effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
            for {
              _ <- escService.removeMember(undertakingRef, removeBE)
              _ <- emailService.sendEmail(EORI(eoriEntered), RemoveMemberToBusinessEntity, undertaking, effectiveDate)
              _ <- emailService.sendEmail(eori, EORI(eoriEntered), RemoveMemberToLead, undertaking, effectiveDate)
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

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmission(u: Undertaking, b: BusinessEntity) =
      removeYourselfBusinessForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(removeYourselfBEPage(errors, b, routes.AccountController.getAccountPage().url)).toContext,
          handleValidFormSubmission(u, b)
        )

    def handleValidFormSubmission(u: Undertaking, b: BusinessEntity)(form: FormValues) =
      if (form.value.isTrue) {
        for {
          _ <- escService.removeMember(u.reference, b).toContext
          _ <- store.deleteAll.toContext
          effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
          _ <- emailService.sendEmail(eori, MemberRemoveSelfToBusinessEntity, u, effectiveDate).toContext
          _ <- emailService.sendEmail(u.getLeadEORI, eori, MemberRemoveSelfToLead, u, effectiveDate).toContext
          _ = auditService.sendEvent(BusinessEntityRemovedSelf(u.reference, request.authorityId, u.getLeadEORI, eori))
        } yield Redirect(routes.SignOutController.signOut())
      } else Redirect(routes.AccountController.getAccountPage()).toContext

    val result = for {
      undertaking <- escService.retrieveUndertaking(eori).toContext
      businessEntityToRemove <- undertaking.findBusinessEntity(eori).toContext
      r <- handleFormSubmission(undertaking, businessEntityToRemove)
    } yield r

    result.getOrElse(handleMissingSessionData("Undertaking"))
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

  // TODO - move to shared location if there are other references to these lengths
  private val validEoriLengths = Set(14, 17) // Valid lengths with 2 letter prefix

  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (validEoriLengths.contains(withGbPrefix(eori).length)) Valid
    else Invalid("businessEntityEori.error.incorrect-length")
  }

  private val isEoriValid = Constraint[String] { eori: String =>
    if (withGbPrefix(eori).matches(EORI.regex)) Valid
    else Invalid("businessEntityEori.regex.error")
  }

  private val eoriForm: Form[FormValues] = Form(
    mapping(
      "businessEntityEori" -> mandatory("businessEntityEori")
        .verifying(isEoriLengthValid)
        .verifying(isEoriValid)
    )(eoriEntered => FormValues(withGbPrefix(eoriEntered)))(eori => eori.value.drop(2).some)
  )

}
