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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ClaimAmount, CurrencyCode}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyAmount}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax.BigDecimalOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmClaimAmountConversionToEuros
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClaimAmountController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  val store: Store,
  val escService: EscService,
  confirmConvertedAmountPage: ConfirmClaimAmountConversionToEuros
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import actionBuilders._

  def getConfirmClaimAmount: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        claimAmount <- subsidyJourney.claimAmount.value.toContext
        claimDate <- subsidyJourney.claimDate.value.toContext
        euroAmount <- convertPoundsToEurosMonthly(claimDate.toLocalDate, claimAmount).toContext
        previous = subsidyJourney.previous
      } yield Ok(confirmConvertedAmountPage(previous, BigDecimal(claimAmount.amount).toPounds, euroAmount.toEuros))
      result.getOrElse(handleMissingSessionData("Subsidy claim amount conversion"))
    }
  }

  def postConfirmClaimAmount: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        claimAmount <- subsidyJourney.claimAmount.value.toContext
        claimDate <- subsidyJourney.claimDate.value.toContext
        euroAmount <- convertPoundsToEurosMonthly(claimDate.toLocalDate, claimAmount).toContext
        updatedSubsidyJourney = subsidyJourney.setConvertedClaimAmount(
          ClaimAmount(EUR, euroAmount.toRoundedAmount.toString())
        )
        _ <- store.put[SubsidyJourney](updatedSubsidyJourney).toContext
        redirect <- updatedSubsidyJourney.next.toContext
      } yield redirect
      result.getOrElse(handleMissingSessionData("Subsidy claim amount conversion"))
    }
  }

  private def convertPoundsToEurosMonthly(date: LocalDate, claimAmount: ClaimAmount)(implicit hc: HeaderCarrier) =
    claimAmount.currencyCode match {
      case GBP =>
        for {
          exchangeRate <- escService.getExchangeRate(date)
          rateOption = exchangeRate.map(_.amount)
          converted = rateOption.map(rate => BigDecimal(claimAmount.amount) / rate)
        } yield converted
      case EUR => Future.successful(None)
    }
}
