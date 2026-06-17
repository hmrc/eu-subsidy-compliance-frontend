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
import play.api.data.{Form, FormError}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.{Errors, Fields}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{AddClaimAmountPage, AddEuroOnlyClaimAmountPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimAmountController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  addClaimAmountPage: AddClaimAmountPage,
  addEuroOnlyClaimAmountPage: AddEuroOnlyClaimAmountPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  def getClaimAmount: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result: OptionT[Future, Result] = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        addClaimDate <- subsidyJourney.claimDate.value.toContext
        previous = subsidyJourney.previous
        exchangeRate <- escService.getExchangeRate(addClaimDate.toLocalDate).toContext
        conversionRate = exchangeRate.amount
        claimAmountForm = ClaimAmountFormProvider(conversionRate).form
      } yield {
        val form = subsidyJourney.claimAmount.value.fold(claimAmountForm) { ca =>
          if (appConfig.euroOnlyEnabled && ca.currencyCode == CurrencyCode.GBP)
            claimAmountForm
          else if (appConfig.euroOnlyEnabled && ca.currencyCode == CurrencyCode.EUR)
            claimAmountForm.fill(ca)
          else claimAmountForm.fill(ca)
        }
        if (appConfig.euroOnlyEnabled)
          Ok(addEuroOnlyClaimAmountPage(form, previous, addClaimDate.year, addClaimDate.month))
        else Ok(addClaimAmountPage(form, previous, addClaimDate.year, addClaimDate.month))
      }
      result.getOrElse(Redirect(routes.ClaimDateController.getClaimDate))
    }
  }

  def postAddClaimAmount: Action[AnyContent] = subsidyJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def badRequest(previous: Journey.Uri, addClaimDate: DateFormValues, formWithErrors: Form[ClaimAmount]) =
      BadRequest(
        if (appConfig.euroOnlyEnabled)
          addEuroOnlyClaimAmountPage(formWithErrors, previous, addClaimDate.year, addClaimDate.month)
        else
          addClaimAmountPage(formWithErrors, previous, addClaimDate.year, addClaimDate.month)
      )

    def handleFormSubmit(
      previous: Journey.Uri,
      addClaimDate: DateFormValues,
      claimAmountForm: Form[ClaimAmount]
    ): Future[Result] =
      claimAmountForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            if (formWithErrors.errors.head.messages.head == "error.incorrect-format") {
              val key = if (formWithErrors.data("currency-code") == "EUR") "claim-amount-eur" else "claim-amount-gbp"
              val newFormWithErrors = formWithErrors.withError(key, "error.incorrect-format")
              Future.successful(
                badRequest(previous, addClaimDate, newFormWithErrors.copy(errors = newFormWithErrors.errors.tail))
              )
            } else {
              Future.successful(badRequest(previous, addClaimDate, formWithErrors))
            },
          claimAmountEntered => {
            val result = for {
              journey <- store.update[SubsidyJourney](_.setClaimAmount(claimAmountEntered)).toContext
              redirect <- journey.next.toContext
            } yield redirect
            result.getOrElse(
              badRequest(
                previous,
                addClaimDate,
                claimAmountForm
                  .bindFromRequest()
                  .withError(FormError(Fields.ClaimAmountGBP, List(Errors.TooBig)))
              )
            )
          }
        )

    withLeadUndertaking { _ =>
      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        addClaimDate <- subsidyJourney.claimDate.value.toContext
        previous = subsidyJourney.previous
        exchangeRate <- escService.getExchangeRate(addClaimDate.toLocalDate).toContext
        conversionRate = exchangeRate.amount
        claimAmountForm = ClaimAmountFormProvider(conversionRate).form
        submissionResult <- handleFormSubmit(previous, addClaimDate, claimAmountForm).toContext
      } yield submissionResult
      result.getOrElse(handleMissingSessionData("Subsidy journey"))
    }
  }
}
