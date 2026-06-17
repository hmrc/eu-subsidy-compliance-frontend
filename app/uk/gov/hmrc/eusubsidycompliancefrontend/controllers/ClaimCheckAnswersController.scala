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
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyAdded
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EisSubsidyAmendmentType.EisSubsidyAmendmentType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyAmount.SubsidyAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TraderRef.TraderRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TraderRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ClaimCheckYourAnswerPage

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimCheckAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  auditService: AuditService,
  cyaPage: ClaimCheckYourAnswerPage,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val cyaForm = formWithSingleMandatoryField("cya")

  def getCheckAnswers: Action[AnyContent] = subsidyJourney.async { implicit request =>
    def getEuroAmount(j: SubsidyJourney) =
      if (j.claimAmountIsInEuros) j.getClaimAmount
      else j.getConvertedClaimAmount

    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result: OptionT[Future, Result] = for {
        journey <- store.get[SubsidyJourney].toContext
        _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
        _ <- store.update[SubsidyJourney](_.setCya(true)).toContext
        claimDate <- journey.claimDate.value.toContext
        euroAmount <- getEuroAmount(journey).toContext
        optionalEori <- journey.addClaimEori.value.toContext
        authority <- journey.publicAuthority.value.toContext
        optionalTraderRef <- journey.traderRef.value.toContext
        traderRef = optionalTraderRef.value.map(TraderRef(_))
        claimEori = optionalEori.value.map(EORI(_))
        previous = journey.previous
      } yield Ok(cyaPage(claimDate, euroAmount, claimEori, authority, traderRef, previous))
      result.getOrElse(Redirect(routes.AccountController.getAccountPage))
    }
  }

  def backFromCheckYourAnswers: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store.update[SubsidyJourney](_.setCya(false)).map { _ =>
        Redirect(routes.AddClaimReferenceController.getAddClaimReference)
      }
    }
  }

  def postCheckAnswers: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      def handleAddBusinessRequest(addBusiness: Boolean, businessEori: Option[EORI]) =
        if (addBusiness)
          businessEori
            .fold(throw new IllegalStateException("No EORI found when add business to undertaking requested")) { be =>
              escService
                .addMember(undertaking.reference, BusinessEntity(be, leadEORI = false))
                .toContext
            }
        else ().toContext

      def handleValidSubmission(form: FormValues) = {
        val result = for {
          journey <- store.update[SubsidyJourney](_.setCya(form.value.toBoolean)).toContext
          _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
          ref <- undertaking.reference.toContext
          currentDate = timeProvider.today
          _ <- escService.createSubsidy(SubsidyController.toSubsidyUpdate(journey, ref, currentDate)).toContext
          _ <- handleAddBusinessRequest(journey.getAddBusiness, journey.getClaimEori)
          _ = auditService.sendEvent[NonCustomsSubsidyAdded](
            AuditEvent.NonCustomsSubsidyAdded(request.authorityId, eori, ref, journey, currentDate)
          )
          _ <- store.update[SubsidyJourney](_.setSubmitted(true)).toContext
          _ <- escService.clearUndertakingCache(ref).toContext
          isSuspended = undertaking.isAutoSuspended
        } yield Redirect(routes.ClaimConfirmationController.getClaimConfirmationPage(isSuspended))
        result.getOrElse(sys.error("Error processing subsidy cya form submission"))
      }

      cyaForm
        .bindFromRequest()
        .fold(
          _ => sys.error("Error processing subsidy cya form submission"),
          form => handleValidSubmission(form)
        )
    }
  }

  private def validateSubsidyJourneyFieldsPopulated(journey: SubsidyJourney): Option[Unit] =
    for {
      _ <- journey.addClaimEori.value.orElse(handleMissingSessionData("claim EORI"))
      _ <- journey.claimAmount.value.orElse(handleMissingSessionData("claim amount"))
      _ <- journey.claimDate.value.orElse(handleMissingSessionData("claim date"))
      _ <- journey.publicAuthority.value.orElse(handleMissingSessionData("public authority"))
      _ <- journey.traderRef.value.orElse(handleMissingSessionData("trader ref"))
    } yield ()
}
