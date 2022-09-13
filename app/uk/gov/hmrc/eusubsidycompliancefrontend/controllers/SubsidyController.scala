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

import cats.data.OptionT
import cats.implicits._
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyController.toSubsidyUpdate
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.{ClaimAmountFormProvider, ClaimDateFormProvider, ClaimEoriFormProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{NonCustomsSubsidyAdded, NonCustomsSubsidyRemoved}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax.BigDecimalOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubsidyController @Inject() (
                                    mcc: MessagesControllerComponents,
                                    actionBuilders: ActionBuilders,
                                    override val store: Store,
                                    override val escService: EscService,
                                    auditService: AuditService,
                                    reportedPaymentsPage: ReportedPaymentsPage,
                                    addClaimEoriPage: AddClaimEoriPage,
                                    addClaimAmountPage: AddClaimAmountPage,
                                    addClaimDatePage: AddClaimDatePage,
                                    addPublicAuthorityPage: AddPublicAuthorityPage,
                                    addTraderReferencePage: AddTraderReferencePage,
                                    cyaPage: ClaimCheckYourAnswerPage,
                                    confirmRemovePage: ConfirmRemoveClaim,
                                    confirmConvertedAmountPage: ConfirmClaimAmountConversionToEuros,
                                    timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with FormHelpers {

  import actionBuilders._

  private val removeSubsidyClaimForm: Form[FormValues] = formWithSingleMandatoryField("removeSubsidyClaim")
  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")

  private val claimTraderRefForm: Form[OptionalTraderRef] = Form(
    mapping(
      "should-store-trader-ref" -> mandatory("should-store-trader-ref"),
      "claim-trader-ref" -> mandatoryIfEqual("should-store-trader-ref", "true", nonEmptyText)
    )(OptionalTraderRef.apply)(OptionalTraderRef.unapply)
  )

  private val claimPublicAuthorityForm: Form[String] = Form(
    "claim-public-authority" -> mandatory("claim-public-authority")
      .verifying("error.claim-public-authority.tooManyChars", _.length < 151)
  )

  private val claimAmountForm: Form[ClaimAmount] = ClaimAmountFormProvider().form

  private val claimDateForm: Form[DateFormValues] = ClaimDateFormProvider(timeProvider).form

  def getReportedPayments: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      val currentDate = timeProvider.today

      retrieveSubsidies(undertaking.reference).map { subsidies =>
        Ok(
          reportedPaymentsPage(
            subsidies,
            undertaking,
            currentDate.toEarliestTaxYearStart,
            currentDate.toTaxYearEnd.minusYears(1),
            currentDate.toTaxYearStart,
            routes.AccountController.getAccountPage().url
          )
        )
      }
    }
  }

  private def retrieveSubsidies(r: UndertakingRef)(implicit request: AuthenticatedEnrolledRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber

    val searchRange = timeProvider.today.toSearchRange.some

    escService
      .retrieveSubsidy(SubsidyRetrieve(r, searchRange))
      .map(Option(_))
      .fallbackTo(Option.empty.toFuture)
  }

  def getClaimDate: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val form = journey.claimDate.value.fold(claimDateForm)(claimDateForm.fill)
        val earliestAllowedClaimDate = timeProvider.today.toEarliestTaxYearStart
        Ok(addClaimDatePage(form, journey.previous, earliestAllowedClaimDate))
      }
    }
  }


  def postClaimDate: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      processFormSubmission[SubsidyJourney] { journey =>
        claimDateForm
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val earliestAllowedClaimDate = timeProvider.today.toEarliestTaxYearStart
              BadRequest(addClaimDatePage(formWithErrors, journey.previous, earliestAllowedClaimDate)).toContext
            },
            form =>
              store.update[SubsidyJourney](_.setClaimDate(form))
                .flatMap(_.next)
                .toContext
          )
      }
    }
  }

  def getClaimAmount: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result: OptionT[Future, Result] = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        addClaimDate <- subsidyJourney.claimDate.value.toContext
        previous = subsidyJourney.previous
      } yield {
        val form = subsidyJourney.claimAmount.value.fold(claimAmountForm)(claimAmountForm.fill)
        Ok(addClaimAmountPage(form, previous, addClaimDate.year, addClaimDate.month))
      }
      result.getOrElse(Redirect(routes.SubsidyController.getClaimDate()))
    }
  }

  def postAddClaimAmount: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmit(previous: Journey.Uri, addClaimDate: DateFormValues): Future[Result] =
      claimAmountForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            BadRequest(addClaimAmountPage(formWithErrors, previous, addClaimDate.year, addClaimDate.month)).toFuture,
          claimAmountEntered => for {
            journey <- store.update[SubsidyJourney](_.setClaimAmount(claimAmountEntered))
            redirect <- journey.next
          } yield redirect
        )

    withLeadUndertaking { _ =>
      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        addClaimDate <- subsidyJourney.claimDate.value.toContext
        previous = subsidyJourney.previous
        resultFormSubmit <- handleFormSubmit(previous, addClaimDate).toContext
      } yield resultFormSubmit
      result.getOrElse(handleMissingSessionData("Subsidy journey"))
    }
  }

  def getConfirmClaimAmount: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori = request.eoriNumber

      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        claimAmount <- subsidyJourney.claimAmount.value.toContext
        claimDate <- subsidyJourney.claimDate.value.toContext
        euroAmount <- convertPoundsToEuros(claimDate.toLocalDate, claimAmount).toContext
        previous = subsidyJourney.previous
      } yield Ok(confirmConvertedAmountPage(previous, BigDecimal(claimAmount.amount).toPounds, euroAmount.toEuros))

      result.getOrElse(handleMissingSessionData("Subsidy claim amount conversion"))
    }
  }

  def postConfirmClaimAmount: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori = request.eoriNumber

      val result = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        claimAmount <- subsidyJourney.claimAmount.value.toContext
        claimDate <- subsidyJourney.claimDate.value.toContext
        euroAmount <- convertPoundsToEuros(claimDate.toLocalDate, claimAmount).toContext
        updatedSubsidyJourney = subsidyJourney.setConvertedClaimAmount(ClaimAmount(EUR, euroAmount.toRoundedAmount.toString()))
        _ <- store.put[SubsidyJourney](updatedSubsidyJourney).toContext
        redirect <- updatedSubsidyJourney.next.toContext
      } yield redirect

      result.getOrElse(handleMissingSessionData("Subsidy claim amount conversion"))
    }
  }

  private def convertPoundsToEuros(date: LocalDate, claimAmount: ClaimAmount)(implicit hc: HeaderCarrier) =
    claimAmount.currencyCode match {
      case GBP =>
        for {
          exchangeRate <- escService.retrieveExchangeRate(date)
          rate = exchangeRate.rate
          converted = BigDecimal(claimAmount.amount) / rate
        } yield converted.some
      case EUR => Future.successful(None)
    }

  def getAddClaimEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      renderFormIfEligible { journey =>
        val claimEoriForm = ClaimEoriFormProvider(undertaking).form
        val updatedForm = journey.addClaimEori.value.fold(claimEoriForm) { optionalEORI =>
          claimEoriForm.fill(OptionalEORI(optionalEORI.setValue, optionalEORI.value))
        }
        Ok(addClaimEoriPage(updatedForm, journey.previous))
      }
    }
  }

  def postAddClaimEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val claimEoriForm = ClaimEoriFormProvider(undertaking).form

      processFormSubmission[SubsidyJourney] { journey =>
        claimEoriForm
          .bindFromRequest()
          .fold(
            formWithErrors => BadRequest(addClaimEoriPage(formWithErrors, journey.previous)).toContext,
            form => store.update[SubsidyJourney](_.setClaimEori(form)).flatMap(_.next).toContext
          )
      }

    }
  }

  def getAddClaimPublicAuthority: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val form = journey.publicAuthority.value.fold(claimPublicAuthorityForm)(claimPublicAuthorityForm.fill)
        Ok(addPublicAuthorityPage(form, journey.previous))
      }
    }
  }

  def postAddClaimPublicAuthority: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      processFormSubmission[SubsidyJourney] { journey =>
        claimPublicAuthorityForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addPublicAuthorityPage(errors, journey.previous)).toContext,
            form => store.update[SubsidyJourney](_.setPublicAuthority(form)).flatMap(_.next).toContext
          )
      }
    }
  }

  def getAddClaimReference: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val form = journey.traderRef.value.fold(claimTraderRefForm) { optionalTraderRef =>
          claimTraderRefForm.fill(OptionalTraderRef(optionalTraderRef.setValue, optionalTraderRef.value))
        }
        Ok(addTraderReferencePage(form, journey.previous))
      }
    }
  }

  def postAddClaimReference: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      processFormSubmission[SubsidyJourney] { journey =>
        claimTraderRefForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addTraderReferencePage(errors, journey.previous)).toContext,
            form => store.update[SubsidyJourney](_.setTraderRef(form)).flatMap(_.next).toContext
          )
      }
    }
  }

  def getCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>

    def getEuroAmount(j: SubsidyJourney) =
      if (j.claimAmountInEuros) j.getClaimAmount
      else j.getConvertedClaimAmount

    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      val result: OptionT[Future, Result] = for {
        journey <- store.get[SubsidyJourney].toContext
        _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
        claimDate <- journey.claimDate.value.toContext
        euroAmount <- getEuroAmount(journey).toContext
        optionalEori <- journey.addClaimEori.value.toContext
        authority <- journey.publicAuthority.value.toContext
        optionalTraderRef <- journey.traderRef.value.toContext
        traderRef = optionalTraderRef.value.map(TraderRef(_))
        claimEori = optionalEori.value.map(EORI(_))
        previous = journey.previous
      } yield Ok(cyaPage(claimDate, euroAmount, claimEori, authority, traderRef, previous))

      result.getOrElse(Redirect(routes.SubsidyController.getAddClaimReference()))
    }
  }

  def postCheckAnswers: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidSubmission(form: FormValues) = {
        val result = for {
          journey <- store.update[SubsidyJourney](_.setCya(form.value.toBoolean)).toContext
          _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
          ref <- undertaking.reference.toContext
          currentDate = timeProvider.today
          _ <- escService.createSubsidy(toSubsidyUpdate(journey, ref, currentDate)).toContext
          _ <- store.put(SubsidyJourney()).toContext
          _ = auditService.sendEvent[NonCustomsSubsidyAdded](
            AuditEvent.NonCustomsSubsidyAdded(request.authorityId, eori, ref, journey, currentDate)
          )
        } yield Redirect(routes.SubsidyController.getReportedPayments())

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

  def getRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEmail.async {
    implicit request =>
      withLeadUndertaking { undertaking =>
        val result = for {
          reference <- undertaking.reference.toContext
          subsidies <- retrieveSubsidies(reference).toContext
          sub <- subsidies.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionId.contains(transactionId)).toContext
        } yield Ok(confirmRemovePage(removeSubsidyClaimForm, sub))
        result.fold(handleMissingSessionData("Subsidy Journey"))(identity)
      }
  }

  def postRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEmail.async {
    implicit request =>
      withLeadUndertaking { undertaking =>
        removeSubsidyClaimForm
          .bindFromRequest()
          .fold(
            formWithErrors => handleRemoveSubsidyFormError(formWithErrors, transactionId, undertaking),
            formValue =>
              if (formValue.value.isTrue) handleRemoveSubsidyValidAnswer(transactionId, undertaking)
              else Redirect(routes.SubsidyController.getReportedPayments()).toFuture
          )
      }
  }

  private def getNonHmrcSubsidy(
    transactionId: String,
    reference: UndertakingRef
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): OptionT[Future, NonHmrcSubsidy] =
    retrieveSubsidies(reference)
      .recoverWith({ case _ => Option.empty[UndertakingSubsidies].toFuture })
      .toContext
      .flatMap(_.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionId.contains(transactionId)).toContext)


  private def handleRemoveSubsidyFormError(
    formWithErrors: Form[FormValues],
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    val result = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
    } yield BadRequest(confirmRemovePage(formWithErrors, nonHmrcSubsidy))

    result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
  }

  private def handleRemoveSubsidyValidAnswer(
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    val result = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
      _ <- escService.removeSubsidy(reference, nonHmrcSubsidy).toContext
      _ = auditService.sendEvent[NonCustomsSubsidyRemoved](
        AuditEvent.NonCustomsSubsidyRemoved(request.authorityId, reference)
      )
    } yield Redirect(routes.SubsidyController.getReportedPayments())

    result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
  }

    protected def renderFormIfEligible(f: SubsidyJourney => Result)(implicit r: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
      implicit val eori: EORI = r.eoriNumber

      store.getOrCreate[SubsidyJourney](SubsidyJourney()).toContext
        .map { journey =>
          if (journey.isEligibleForStep) f(journey)
          else Redirect(journey.previous)
        }
        .getOrElse(Redirect(routes.SubsidyController.getReportedPayments().url))

    }

}

object SubsidyController {
  def toSubsidyUpdate(
    journey: SubsidyJourney,
    undertakingRef: UndertakingRef,
    currentDate: LocalDate
  ): SubsidyUpdate =
    SubsidyUpdate(
      undertakingIdentifier = undertakingRef,
      UndertakingSubsidyAmendment(
        List(
          NonHmrcSubsidy(
            subsidyUsageTransactionId = journey.existingTransactionId,
            allocationDate = journey.claimDate.value
              .map(_.toLocalDate)
              .getOrElse(throw new IllegalStateException("No claimdate on SubsidyJourney")),
            submissionDate = currentDate,
            // this shouldn't be optional, is required in create API but not retrieve
            publicAuthority = Some(journey.publicAuthority.value.get),
            traderReference = journey.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
            nonHMRCSubsidyAmtEUR =
              if (journey.claimAmountInEuros)
                SubsidyAmount(journey.getClaimAmount.getOrElse(sys.error("Claim amount Missing")))
              else
                SubsidyAmount(journey.getConvertedClaimAmount.getOrElse(sys.error("Converted claim amount Missing"))),
              businessEntityIdentifier = journey.addClaimEori.value.fold(sys.error("eori value missing"))(oprionalEORI =>
              oprionalEORI.value.map(EORI(_))
            ),
            amendmentType = journey.existingTransactionId.fold(Some(EisSubsidyAmendmentType("1")))(_ =>
              Some(EisSubsidyAmendmentType("2"))
            )
          )
        )
      )
    )
}
