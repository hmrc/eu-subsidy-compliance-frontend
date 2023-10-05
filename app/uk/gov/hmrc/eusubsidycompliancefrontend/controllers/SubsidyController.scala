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
import cats.implicits._
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyController.toSubsidyUpdate
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.{Errors, Fields}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField, mandatory}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.IncorrectFormat
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.{ClaimAmountFormProvider, ClaimDateFormProvider, ClaimEoriFormProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{Journey, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{NonCustomsSubsidyAdded, NonCustomsSubsidyRemoved}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax.BigDecimalOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.formatEori

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubsidyController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  exchangeRateService: ExchangeRateService,
  auditService: AuditService,
  reportedPaymentsPage: ReportedPaymentsPage,
  addClaimEoriPage: AddClaimEoriPage,
  addClaimBusinessPage: AddClaimBusinessPage,
  addClaimAmountPage: AddClaimAmountPage,
  addEuroOnlyClaimAmountPage: AddEuroOnlyClaimAmountPage,
  reportPaymentFirstTimeUserPage: ReportPaymentFirstTimeUserPage,
  reportedPaymentReturningUserPage: ReportedPaymentReturningUserPage,
  reportNonCustomSubsidyPage: ReportNonCustomSubsidyPage,
  addClaimDatePage: AddClaimDatePage,
  addPublicAuthorityPage: AddPublicAuthorityPage,
  addTraderReferencePage: AddTraderReferencePage,
  cyaPage: ClaimCheckYourAnswerPage,
  confirmCreatedPage: ClaimConfirmationPage,
  confirmRemovePage: ConfirmRemoveClaim,
  confirmConvertedAmountPage: ConfirmClaimAmountConversionToEuros,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val removeSubsidyClaimForm: Form[FormValues] = formWithSingleMandatoryField("removeSubsidyClaim")
  private val cyaForm: Form[FormValues] = formWithSingleMandatoryField("cya")
  private val addClaimBusinessForm: Form[FormValues] = formWithSingleMandatoryField("add-claim-business")
  private val reportedPaymentFirstTimeUserForm: Form[FormValues] = formWithSingleMandatoryField(
    "reportPaymentFirstTimeUser"
  )
  private val reportedPaymentNonCustomSubsidyForm: Form[FormValues] = formWithSingleMandatoryField(
    "reportNonCustomSubsidy"
  )
  private val reportedPaymentReturningUserForm: Form[FormValues] = formWithSingleMandatoryField(
    "report-payment-return"
  )

  private val enteredTradingRefIsValid = Constraint[String] { traderRef: String =>
    if (traderRef.matches(TraderRef.regex)) Valid
    else Invalid(IncorrectFormat)
  }

  private val claimTraderRefForm: Form[OptionalTraderRef] = Form(
    mapping(
      "should-store-trader-ref" -> mandatory("should-store-trader-ref"),
      "claim-trader-ref" -> mandatoryIfEqual(
        "should-store-trader-ref",
        "true",
        text.verifying(enteredTradingRefIsValid)
      )
    )(OptionalTraderRef.apply)(OptionalTraderRef.unapply)
  )

  private val claimPublicAuthorityForm: Form[String] = Form(
    "claim-public-authority" -> mandatory("claim-public-authority")
  )

  private val claimAmountForm: Form[ClaimAmount] = ClaimAmountFormProvider().form

  private val claimDateForm: Form[DateFormValues] = ClaimDateFormProvider(timeProvider).form

  def getReportedPayments: Action[AnyContent] = verifiedEori.async { implicit request =>
    logger.info("SelectNewLeadController.getReportedPayments")

    withLeadUndertaking(renderReportedPaymentsPage(_))
  }

  private def renderReportedPaymentsPage(
    undertaking: Undertaking,
    showSuccess: Boolean = false
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber

    val currentDate = timeProvider.today

    escService
      .retrieveSubsidiesForDateRange(undertaking.reference, currentDate.toSearchRange)
      .map { subsidies =>
        Ok(
          reportedPaymentsPage(
            subsidies.forReportedPaymentsPage,
            undertaking,
            currentDate.toEarliestTaxYearStart,
            currentDate.toTaxYearEnd.minusYears(1),
            currentDate.toTaxYearStart,
            routes.AccountController.getAccountPage.url,
            showSuccessBanner = showSuccess
          )
        )
      }
  }

  def getReportPaymentFirstTimeUser: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.reportPaymentFirstTimeUser.value
            .fold(reportedPaymentFirstTimeUserForm)(v => reportedPaymentFirstTimeUserForm.fill(FormValues(v)))
        val today = timeProvider.today
        val startDate = today.toEarliestTaxYearStart
        Ok(
          reportPaymentFirstTimeUserPage(
            updatedForm,
            startDate.toDisplayFormat,
            routes.AccountController.getAccountPage.url
          )
        )
      }
    }
  }

  def postReportPaymentFirstTimeUser: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidFormSubmission(f: FormValues): OptionT[Future, Result] = {
        val userSelection = f.value.isTrue
        store.update[SubsidyJourney](_.setReportPaymentFirstTimeUser(userSelection)).toContext.map { updatedJourney =>
          if (userSelection) Redirect(routes.SubsidyController.getReportedNoCustomSubsidyPage)
          else Redirect(routes.NoClaimNotificationController.getNoClaimNotification)
        }
      }

      val today = timeProvider.today
      val startDate = today.toEarliestTaxYearStart
      processFormSubmission[SubsidyJourney] { journey =>
        reportedPaymentFirstTimeUserForm
          .bindFromRequest()
          .fold(
            errors =>
              BadRequest(
                reportPaymentFirstTimeUserPage(
                  errors,
                  startDate.toDisplayFormat,
                  routes.AccountController.getAccountPage.url
                )
              ).toContext,
            handleValidFormSubmission
          )
      }
    }
  }

  def startFirstTimeUserJourney: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store
        .put[SubsidyJourney](SubsidyJourney())
        .map(_ => Redirect(routes.SubsidyController.getReportPaymentFirstTimeUser.url))
    }
  }

  def startJourney: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store
        .put[SubsidyJourney](SubsidyJourney())
        .map(_ => Redirect(routes.SubsidyController.getReportedPaymentReturningUserPage.url))
    }
  }

  def getReportedPaymentReturningUserPage: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.reportPaymentReturningUser.value
            .fold(reportedPaymentReturningUserForm)(v => reportedPaymentReturningUserForm.fill(FormValues(v)))

        Ok(reportedPaymentReturningUserPage(updatedForm, routes.AccountController.getAccountPage.url))
      }
    }
  }

  def postReportedPaymentReturningUserPage: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidFormSubmission(f: FormValues): OptionT[Future, Result] = {
        val userSelection = f.value.isTrue
        store.update[SubsidyJourney](_.setReportedPaymentReturningUser(userSelection)).toContext.map { _ =>
          if (userSelection)
            Redirect(routes.SubsidyController.getReportedNoCustomSubsidyPage)
          else Redirect(routes.NoClaimNotificationController.getNoClaimNotification)
        }
      }

      processFormSubmission[SubsidyJourney] { journey =>
        reportedPaymentReturningUserForm
          .bindFromRequest()
          .fold(
            errors =>
              BadRequest(
                reportedPaymentReturningUserPage(errors, routes.SubsidyController.getReportPaymentFirstTimeUser.url)
              ).toContext,
            handleValidFormSubmission
          )
      }
    }
  }

  def getReportedNoCustomSubsidyPage: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.reportedNonCustomSubsidy.value
            .fold(reportedPaymentNonCustomSubsidyForm)(v => reportedPaymentNonCustomSubsidyForm.fill(FormValues(v)))

        val previousUrl =
          if (journey.getReportPaymentReturningUser)
            routes.SubsidyController.getReportedPaymentReturningUserPage.url
          else routes.SubsidyController.getReportPaymentFirstTimeUser.url

        val currentYear = LocalDate.now.getYear
        Ok(
          reportNonCustomSubsidyPage(
            updatedForm,
            previousUrl
          )
        )
      }
    }
  }

  def getClaimDate: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      logger.info("SelectNewLeadController.getClaimDate")

      renderFormIfEligible { journey =>
        val form = journey.claimDate.value.fold(claimDateForm)(claimDateForm.fill)
        val earliestAllowedClaimDate = timeProvider.today.toEarliestTaxYearStart
        Ok(
          addClaimDatePage(form, routes.SubsidyController.getReportedNoCustomSubsidyPage.url, earliestAllowedClaimDate)
        )
      }
    }
  }

  def postClaimDate: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("SelectNewLeadController.postClaimDate")

      processFormSubmission[SubsidyJourney] { journey =>
        claimDateForm
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val earliestAllowedClaimDate = timeProvider.today.toEarliestTaxYearStart
              BadRequest(addClaimDatePage(formWithErrors, journey.previous, earliestAllowedClaimDate)).toContext
            },
            form =>
              store
                .update[SubsidyJourney](_.setClaimDate(form))
                .flatMap(_.next)
                .toContext
          )
      }
    }
  }

  def getClaimAmount: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result: OptionT[Future, Result] = for {
        subsidyJourney <- store.get[SubsidyJourney].toContext
        addClaimDate <- subsidyJourney.claimDate.value.toContext
        previous = subsidyJourney.previous
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
      result.getOrElse(Redirect(routes.SubsidyController.getClaimDate))
    }
  }

  def postAddClaimAmount: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def badRequest(previous: Uri, addClaimDate: DateFormValues, formWithErrors: Form[ClaimAmount]) = {
      BadRequest(
        if (appConfig.euroOnlyEnabled)
          addEuroOnlyClaimAmountPage(
            formWithErrors,
            previous,
            addClaimDate.year,
            addClaimDate.month
          )
        else
          addClaimAmountPage(
            formWithErrors,
            previous,
            addClaimDate.year,
            addClaimDate.month
          )
      )
    }

    def handleFormSubmit(previous: Journey.Uri, addClaimDate: DateFormValues): Future[Result] =
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
              _ <- validateClaimAmount(addClaimDate.toLocalDate, claimAmountEntered).toContext
              journey <- store.update[SubsidyJourney](_.setClaimAmount(claimAmountEntered)).toContext
              redirect <- journey.next.toContext
            } yield redirect

            result.getOrElse(
              // The error case here will be due to failure to convert the converted amount into a SubsidyAmount.
              // In this instance we report this as a 'tooBig' error in the form for the user to action.
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
        submissionResult <- handleFormSubmit(previous, addClaimDate).toContext
      } yield submissionResult
      result.getOrElse(handleMissingSessionData("Subsidy journey"))
    }
  }

  def getConfirmClaimAmount: Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def postConfirmClaimAmount: Action[AnyContent] = verifiedEori.async { implicit request =>
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
          exchangeRate <- exchangeRateService.retrieveCachedMonthlyExchangeRate(date)
          rateOption = exchangeRate.map(_.amount)
          converted = rateOption.map(rate => BigDecimal(claimAmount.amount) / rate)
        } yield converted
      case EUR => Future.successful(None)
    }

  private def validateClaimAmount(date: LocalDate, claimAmount: ClaimAmount)(implicit hc: HeaderCarrier) =
    claimAmount.currencyCode match {
      case GBP =>
        for {
          exchangeRate <- exchangeRateService.retrieveCachedMonthlyExchangeRate(date)
          rateOption = exchangeRate.map(_.amount)
          converted = rateOption.map(rate => BigDecimal(claimAmount.amount) / rate)
        } yield SubsidyAmount
          .validateAndTransform(converted.getOrElse(BigDecimal(0)).toRoundedAmount)
          .map(_ => claimAmount)
      case EUR => claimAmount.some.toFuture
    }

  def getAddClaimEori: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { undertaking =>
      renderFormIfEligible { journey =>
        val claimEoriForm = ClaimEoriFormProvider(undertaking).form
        val updatedForm = journey.addClaimEori.value.fold(claimEoriForm) { optionalEORI =>
          claimEoriForm.fill(OptionalClaimEori(optionalEORI.setValue, optionalEORI.value))
        }
        val previous =
          if (appConfig.euroOnlyEnabled) routes.SubsidyController.getClaimAmount.url
          else journey.previous
        Ok(addClaimEoriPage(updatedForm, previous))
      }
    }
  }

  def postAddClaimEori: Action[AnyContent] = verifiedEori.async { implicit request =>
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
          // Default case. Should never happen if form submitted via our frontend.
          case _ => Redirect(routes.SubsidyController.getAddClaimEori).toFuture
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

  def getAddClaimBusiness: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.addClaimBusiness.value
            .fold(addClaimBusinessForm)(v => addClaimBusinessForm.fill(FormValues(v)))

        Ok(addClaimBusinessPage(updatedForm, journey.previous))
      }
    }
  }

  def postAddClaimBusiness: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidFormSubmission(f: FormValues): OptionT[Future, Result] =
        for {
          updatedJourney <- store.update[SubsidyJourney](_.setAddBusiness(f.value.isTrue)).toContext
          next <- updatedJourney.next.toContext
          updateBusiness = updatedJourney.getAddBusiness
        } yield if (updateBusiness) next else Redirect(routes.SubsidyController.getAddClaimEori)

      processFormSubmission[SubsidyJourney] { journey =>
        addClaimBusinessForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addClaimBusinessPage(errors, journey.previous)).toContext,
            handleValidFormSubmission
          )
      }
    }
  }

  def getAddClaimPublicAuthority: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val form = journey.publicAuthority.value.fold(claimPublicAuthorityForm)(claimPublicAuthorityForm.fill)
        Ok(addPublicAuthorityPage(form, journey.previous))
      }
    }
  }

  def postAddClaimPublicAuthority: Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def getAddClaimReference: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val form = journey.traderRef.value.fold(claimTraderRefForm) { optionalTraderRef =>
          claimTraderRefForm.fill(OptionalTraderRef(optionalTraderRef.setValue, optionalTraderRef.value))
        }
        Ok(addTraderReferencePage(form, journey.previous))
      }
    }
  }

  def postAddClaimReference: Action[AnyContent] = verifiedEori.async { implicit request =>
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

  def getCheckAnswers: Action[AnyContent] = verifiedEori.async { implicit request =>
    def getEuroAmount(j: SubsidyJourney) =
      if (j.claimAmountIsInEuros) j.getClaimAmount
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

      result
        .getOrElse(Redirect(routes.AccountController.getAccountPage))
    }
  }

  def postCheckAnswers: Action[AnyContent] = verifiedEori.async { implicit request =>
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
          _ <- escService.createSubsidy(toSubsidyUpdate(journey, ref, currentDate)).toContext
          _ <- handleAddBusinessRequest(journey.getAddBusiness, journey.getClaimEori)
          _ <- store.delete[SubsidyJourney].toContext
          _ = auditService.sendEvent[NonCustomsSubsidyAdded](
            AuditEvent.NonCustomsSubsidyAdded(request.authorityId, eori, ref, journey, currentDate)
          )
        } yield Redirect(routes.SubsidyController.getClaimConfirmationPage)

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

  def getClaimConfirmationPage: Action[AnyContent] = verifiedEori.async { implicit request =>
    val nextClaimDueDate = ReportReminderHelpers.dueDateToReport(timeProvider.today)
    Ok(confirmCreatedPage(nextClaimDueDate)).toFuture
  }

  def getRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withLeadUndertaking { undertaking =>
      val result = for {
        reference <- undertaking.reference.toContext
        subsidies <- escService.retrieveSubsidiesForDateRange(reference, timeProvider.today.toSearchRange).toContext
        sub <- subsidies.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionId.contains(transactionId)).toContext
      } yield Ok(confirmRemovePage(removeSubsidyClaimForm, sub))
      result.fold(handleMissingSessionData("Subsidy Journey"))(identity)
    }
  }

  def postRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { undertaking =>
      removeSubsidyClaimForm
        .bindFromRequest()
        .fold(
          formWithErrors => handleRemoveSubsidyFormError(formWithErrors, transactionId, undertaking),
          formValue =>
            if (formValue.value.isTrue) handleRemoveSubsidyValidAnswer(transactionId, undertaking)
            else Redirect(routes.SubsidyController.getReportedPayments).toFuture
        )
    }
  }

  private def getNonHmrcSubsidy(
    transactionId: String,
    reference: UndertakingRef
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): OptionT[Future, NonHmrcSubsidy] = {
    implicit val e: EORI = r.eoriNumber
    escService
      .retrieveSubsidiesForDateRange(reference, timeProvider.today.toSearchRange)
      .toContext
      .flatMap(_.findNonHmrcSubsidy(transactionId).toContext)
  }

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
    implicit val eori: EORI = request.eoriNumber

    val result: OptionT[Future, Unit] = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
      _ <- escService.removeSubsidy(reference, nonHmrcSubsidy).toContext
      _ = auditService.sendEvent[NonCustomsSubsidyRemoved](
        AuditEvent.NonCustomsSubsidyRemoved(request.authorityId, reference)
      )
    } yield ()

    result.foldF(handleMissingSessionData("nonHMRC subsidy")) { _ =>
      renderReportedPaymentsPage(undertaking, showSuccess = true)
    }
  }

  protected def renderFormIfEligible(
    f: SubsidyJourney => Result
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber

    store
      .getOrCreate[SubsidyJourney](SubsidyJourney())
      .toContext
      .map { journey =>
        if (journey.isEligibleForStep) f(journey)
        else Redirect(journey.previous)
      }
      .getOrElse(Redirect(routes.SubsidyController.getReportedPayments.url))

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
              if (journey.claimAmountIsInEuros)
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
