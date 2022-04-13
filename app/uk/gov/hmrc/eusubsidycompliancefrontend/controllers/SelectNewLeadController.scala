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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromoted
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectNewLeadController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  override val escService: EscService,
  override val store: Store,
  sendEmailHelperService: SendEmailHelperService,
  auditService: AuditService,
  selectNewLeadPage: SelectNewLeadPage,
  leadEORIChangedPage: LeadEORIChangedPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import escActionBuilders._

  private val promoteOtherAsLeadEmailToBusinessEntity = "promoteAsLeadEmailToBE"
  private val promoteOtherAsLeadEmailToLead = "promoteAsLeadEmailToLead"

  private val selectNewLeadForm: Form[FormValues] = Form(
    mapping("selectNewLead" -> mandatory("selectNewLead"))(FormValues.apply)(FormValues.unapply)
  )

  def getSelectNewLead: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      val previous = routes.AccountController.getAccountPage().url
      implicit val eori = request.eoriNumber

      val result = for {
        journey <- store.getOrCreate[NewLeadJourney](NewLeadJourney()).toContext
        form = journey.selectNewLead.value.fold(selectNewLeadForm)(e => selectNewLeadForm.fill(FormValues(e)))
      } yield Ok(selectNewLeadPage(form, previous, undertaking.name, undertaking.getAllNonLeadEORIs()))

      result.getOrElse(handleMissingSessionData("NewLeadJourney"))
    }
  }

  def postSelectNewLead: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      def handleFormErrors(errors: Form[FormValues]) =
        BadRequest(
          selectNewLeadPage(
            errors,
            routes.AccountController.getAccountPage().url,
            undertaking.name,
            undertaking.getAllNonLeadEORIs()
          )
        ).toFuture

      def handleFormSubmission(form: FormValues) = {
        val eoriBE = EORI(form.value)
        val undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("Undertaking Ref"))
        for {
          _ <- store.update[NewLeadJourney] { newLeadJourney =>
            val updatedLead = newLeadJourney.selectNewLead.copy(value = eoriBE.some)
            newLeadJourney.copy(selectNewLead = updatedLead)
          }
          _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
            eoriBE,
            None,
            promoteOtherAsLeadEmailToBusinessEntity,
            undertaking,
            undertakingRef,
            None
          )
          _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
            eori,
            eoriBE.some,
            promoteOtherAsLeadEmailToLead,
            undertaking,
            undertakingRef,
            None
          )
          _ = auditService.sendEvent(BusinessEntityPromoted(undertakingRef, request.authorityId, eori, eoriBE))
        } yield Redirect(routes.SelectNewLeadController.getLeadEORIChanged())
      }

      selectNewLeadForm
        .bindFromRequest()
        .fold(
          handleFormErrors,
          handleFormSubmission
        )
    }
  }

  def getLeadEORIChanged = withAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      store.get[NewLeadJourney].flatMap {
        case Some(newLeadJourney) =>
          for {
            _ <- store.update[BusinessEntityJourney](b => b.copy(isLeadSelectJourney = None))
            _ <- store.put[NewLeadJourney](NewLeadJourney())
            selectedEORI = newLeadJourney.selectNewLead.value.getOrElse(handleMissingSessionData("selected EORI"))
          } yield Ok(leadEORIChangedPage(selectedEORI, undertaking.name))
        case None => handleMissingSessionData("New Lead journey")
      }
    }
  }

}
