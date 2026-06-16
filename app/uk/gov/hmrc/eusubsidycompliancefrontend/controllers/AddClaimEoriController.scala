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

import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimEoriFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.OptionalClaimEori
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.formatEori
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.AddClaimEoriPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddClaimEoriController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  addClaimEoriPage: AddClaimEoriPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers
    with SubsidyJourneySupport {

  import actionBuilders._

  def getAddClaimEori: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { undertaking =>
      renderFormIfEligible { journey =>
        val claimEoriForm = ClaimEoriFormProvider(undertaking).form
        val updatedForm = journey.addClaimEori.value.fold(claimEoriForm) { optionalEORI =>
          claimEoriForm.fill(OptionalClaimEori(optionalEORI.setValue, optionalEORI.value))
        }
        val previous =
          if (appConfig.euroOnlyEnabled) routes.ClaimAmountController.getClaimAmount.url
          else journey.previous
        Ok(addClaimEoriPage(updatedForm, previous))
      }
    }
  }

  def postAddClaimEori: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val claimEoriForm = ClaimEoriFormProvider(undertaking).form

      def storeOptionalEoriAndRedirect(o: OptionalClaimEori) =
        store
          .update[SubsidyJourney](_.setClaimEori(o))
          .flatMap(_.next)

      def handleValidFormSubmission(j: SubsidyJourney, o: OptionalClaimEori): Future[Result] =
        o match {
          case OptionalClaimEori("false", None, _) => storeOptionalEoriAndRedirect(o)
          case OptionalClaimEori("true", Some(e), _) =>
            val enteredEori = EORI(formatEori(e))
            if (undertaking.hasEORI(enteredEori)) storeOptionalEoriAndRedirect(o)
            else
              escService
                .retrieveUndertaking(enteredEori)
                .toContext
                .foldF(storeOptionalEoriAndRedirect(o.copy(addToUndertaking = true))) { _ =>
                  BadRequest(
                    addClaimEoriPage(
                      claimEoriForm
                        .bindFromRequest()
                        .withError("claim-eori", ClaimEoriFormProvider.Errors.InAnotherUndertaking),
                      j.previous
                    )
                  ).toFuture
                }
          case _ => Redirect(routes.AddClaimEoriController.getAddClaimEori).toFuture
        }

      processFormSubmission[SubsidyJourney] { journey =>
        claimEoriForm
          .bindFromRequest()
          .fold(
            formWithErrors => BadRequest(addClaimEoriPage(formWithErrors, journey.previous)).toContext,
            optionalEori => handleValidFormSubmission(journey, removeSpacesFromEnteredEori(optionalEori)).toContext
          )
      }
    }
  }

  private def removeSpacesFromEnteredEori(optionalEori: OptionalClaimEori) =
    optionalEori.copy(value = optionalEori.value.map(_.replaceAll(" ", "")))
}
