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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{BusinessEntityJourney, NewLeadJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromoted
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.{EmailNotSent, EmailSent}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedOtherAsLeadToBusinessEntity, PromotedOtherAsLeadToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectNewLeadController @Inject() (
                                          mcc: MessagesControllerComponents,
                                          actionBuilders: ActionBuilders,
                                          override val escService: EscService,
                                          override val store: Store,
                                          emailService: EmailService,
                                          auditService: AuditService,
                                          selectNewLeadPage: SelectNewLeadPage,
                                          leadEORIChangedPage: LeadEORIChangedPage,
                                          emailNotVerifiedForLeadPromotionPage: EmailNotVerifiedForLeadPromotionPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with ControllerFormHelpers
    with LeadOnlyUndertakingSupport {

  import actionBuilders._

  private val selectNewLeadForm: Form[FormValues] = formWithSingleMandatoryField("selectNewLead")

  def getSelectNewLead: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      val previous = routes.AccountController.getAccountPage.url
      implicit val eori: EORI = request.eoriNumber

      val result = for {
        journey <- store.getOrCreate[NewLeadJourney](NewLeadJourney()).toContext
        form = journey.selectNewLead.value.fold(selectNewLeadForm)(e => selectNewLeadForm.fill(FormValues(e)))
      } yield Ok(selectNewLeadPage(form, previous, undertaking.getAllNonLeadEORIs))

      result.getOrElse(handleMissingSessionData("NewLeadJourney"))
    }
  }

  def postSelectNewLead: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      def handleFormErrors(errors: Form[FormValues]) =
        BadRequest(
          selectNewLeadPage(
            errors,
            routes.AccountController.getAccountPage.url,
            undertaking.getAllNonLeadEORIs
          )
        ).toFuture

      def handleFormSubmission(form: FormValues) = {
        val eoriBE = EORI(form.value)
        val undertakingRef = undertaking.reference
        for {
          _ <- store.update[NewLeadJourney] { newLeadJourney =>
            val updatedLead = newLeadJourney.selectNewLead.copy(value = eoriBE.some)
            newLeadJourney.copy(selectNewLead = updatedLead)
          }
          _ = auditService.sendEvent(BusinessEntityPromoted(undertakingRef, request.authorityId, eori, eoriBE))
          _ <- emailService.sendEmail(eori, eoriBE, PromotedOtherAsLeadToLead, undertaking)
          result <- emailService.sendEmail(eoriBE, PromotedOtherAsLeadToBusinessEntity, undertaking)
            .map(redirectTo)
        } yield result
      }

      selectNewLeadForm
        .bindFromRequest()
        .fold(
          handleFormErrors,
          handleFormSubmission
        )
    }
  }
  private def redirectTo(emailResult: EmailSendResult) = emailResult match {
    case EmailNotSent => Redirect(routes.SelectNewLeadController.emailNotVerified)
    case EmailSent => Redirect(routes.SelectNewLeadController.getLeadEORIChanged)
    case _ => handleMissingSessionData("Email result Response")
  }

  def getLeadEORIChanged = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      store.get[NewLeadJourney].flatMap {
        case Some(newLeadJourney) =>
          for {
            _ <- store.update[BusinessEntityJourney](b => b.copy(isLeadSelectJourney = None))
            _ <- store.put[NewLeadJourney](NewLeadJourney())
            selectedEORI = newLeadJourney.selectNewLead.value
          } yield selectedEORI.fold(Redirect(newLeadJourney.previous))(eori =>
            Ok(leadEORIChangedPage(eori))
          )
        case None => Redirect(routes.SelectNewLeadController.getSelectNewLead).toFuture
      }
    }
  }

  def emailNotVerified = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[NewLeadJourney].flatMap {
      case Some(newLeadJourney) =>
        val beEori =
          newLeadJourney.selectNewLead.value.getOrElse(handleMissingSessionData("Selected EORi for promotion"))
        store.put[NewLeadJourney](NewLeadJourney()).map(_ => Ok(emailNotVerifiedForLeadPromotionPage(beEori)))
      case None => Redirect(routes.SelectNewLeadController.getSelectNewLead).toFuture
    }
  }

}
