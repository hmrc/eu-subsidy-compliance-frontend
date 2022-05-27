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
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyController.toSubsidyUpdate
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.{ClaimDateFormProvider, ClaimEoriFormProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{NonCustomsSubsidyAdded, NonCustomsSubsidyRemoved, NonCustomsSubsidyUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.getValidClaimAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SubsidyController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  store: Store,
  override val escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  auditService: AuditService,
  reportPaymentPage: ReportPaymentPage,
  addClaimEoriPage: AddClaimEoriPage,
  addClaimAmountPage: AddClaimAmountPage,
  addClaimDatePage: AddClaimDatePage,
  addPublicAuthorityPage: AddPublicAuthorityPage,
  addTraderReferencePage: AddTraderReferencePage,
  cyaPage: ClaimCheckYourAnswerPage,
  confirmRemovePage: ConfirmRemoveClaim,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import escCDSActionBuilder._

  private val reportPaymentForm: Form[FormValues] = Form(
    mapping("reportPayment" -> mandatory("reportPayment"))(FormValues.apply)(FormValues.unapply)
  )

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

  private val isClaimAmountTooBig = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    if (amount.length < 17) Valid else Invalid("error.amount.tooBig")
  }

  private val isClaimAmountFormatCorrect = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid("error.amount.incorrectFormat"),
      amount => if (amount.scale == 2 || amount.scale == 0) Valid else Invalid("error.amount.incorrectFormat")
    )
  }

  private val isClaimAmountTooSmall = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid("error.amount.incorrectFormat"),
      amount => if (amount > 0.01) Valid else Invalid("error.amount.tooSmall")
    )
  }

  private val claimAmountForm: Form[BigDecimal] = Form(
    mapping(
      "claim-amount" -> nonEmptyText
        .verifying(isClaimAmountTooBig)
        .verifying(isClaimAmountFormatCorrect)
        .verifying(isClaimAmountTooSmall)
    )(amt => BigDecimal(getValidClaimAmount(amt)))(a => Some(a.toString))
  )

  private val claimDateForm = ClaimDateFormProvider(timeProvider).form

  private val removeSubsidyClaimForm: Form[FormValues] = Form(
    mapping("removeSubsidyClaim" -> mandatory("removeSubsidyClaim"))(FormValues.apply)(FormValues.unapply)
  )

  private val cyaForm: Form[FormValues] = Form(mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

  def getReportPayment: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      val result: OptionT[Future, Future[Result]] = for {
        journey <- store.getOrCreate[SubsidyJourney](SubsidyJourney()).toContext
        reference <- undertaking.reference.toContext
      } yield {
        val form = journey.reportPayment.value
          .fold(reportPaymentForm)(bool => reportPaymentForm.fill(FormValues(bool.toString)))

        val currentDate = timeProvider.today

        retrieveSubsidies(reference).map { subsidies =>
          Ok(
            reportPaymentPage(
              form,
              subsidies,
              undertaking,
              currentDate.toEarliestTaxYearStart,
              currentDate.toTaxYearEnd.minusYears(1),
              currentDate.toTaxYearStart,
              journey.previous
            )
          )
        }
      }

      result.foldF(handleMissingSessionData("Subsidy Journey"))(identity)
    }
  }

  private def retrieveSubsidies(r: UndertakingRef)(implicit request: AuthenticatedEscRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber

    val searchRange = timeProvider.today.toSearchRange.some

    escService
      .retrieveSubsidy(SubsidyRetrieve(r, searchRange))
      .map(Option(_))
      .fallbackTo(Option.empty.toFuture)
  }

  def postReportPayment: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val previous = routes.AccountController.getAccountPage().url
      reportPaymentForm
        .bindFromRequest()
        .fold(
          formWithErrors => handleReportPaymentFormError(previous, undertaking, formWithErrors),
          form =>
            for {
              journey <- store.update[SubsidyJourney](_.setReportPayment(form.value.toBoolean))
              redirect <-
                if (form.value === "true") journey.next
                else Redirect(routes.AccountController.getAccountPage()).toFuture
            } yield redirect
        )
    }
  }

  def getClaimAmount: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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

  def postAddClaimAmount: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmit(previous: Journey.Uri, addClaimDate: DateFormValues) =
      claimAmountForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            BadRequest(addClaimAmountPage(formWithErrors, previous, addClaimDate.year, addClaimDate.month)).toFuture,
          claimAmountEntered =>
            for {
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

  def getClaimDate: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store.get[SubsidyJourney].flatMap {
        case Some(journey) =>
          journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
            val form = journey.claimDate.value.fold(claimDateForm)(claimDateForm.fill)
            if (!journey.isEligibleForStep) {
              Redirect(journey.previous).toFuture
            } else {
              Ok(addClaimDatePage(form, previous)).toFuture
            }
          }
        case _ => Redirect(routes.SubsidyController.getReportPayment()).toFuture
      }
    }
  }

  def postClaimDate: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      getPrevious[SubsidyJourney](store).flatMap { previous =>
        claimDateForm
          .bindFromRequest()
          .fold(
            formWithErrors => BadRequest(addClaimDatePage(formWithErrors, previous)).toFuture,
            form =>
              for {
                journey <- store.update[SubsidyJourney](_.setClaimDate(form))
                redirect <- journey.next
              } yield redirect
          )
      }
    }
  }

  def getAddClaimEori: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val claimEoriForm = ClaimEoriFormProvider(undertaking).form
      store.get[SubsidyJourney].flatMap {
        case Some(journey) =>
          journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
            if (!journey.isEligibleForStep) {
              Redirect(routes.SubsidyController.getClaimDate()).toFuture
            } else {
              val form = journey.addClaimEori.value.fold(claimEoriForm) { optionalEORI =>
                claimEoriForm.fill(OptionalEORI(optionalEORI.setValue, optionalEORI.value))
              }
              Ok(addClaimEoriPage(form, previous)).toFuture
            }
          }
        case _ => Redirect(routes.SubsidyController.getReportPayment()).toFuture
      }
    }
  }

  def postAddClaimEori: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val claimEoriForm = ClaimEoriFormProvider(undertaking).form
      journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
        claimEoriForm
          .bindFromRequest()
          .fold(
            formWithErrors => BadRequest(addClaimEoriPage(formWithErrors, previous)).toFuture,
            (form: OptionalEORI) =>
              for {
                journey <- store.update[SubsidyJourney](_.setClaimEori(form))
                redirect <- journey.next
              } yield redirect
          )
      }
    }
  }

  def getAddClaimPublicAuthority: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store.get[SubsidyJourney].flatMap {
        case Some(journey) =>
          journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
            if (!journey.isEligibleForStep) {
              Redirect(journey.previous).toFuture
            } else {
              val form = journey.publicAuthority.value.fold(claimPublicAuthorityForm)(claimPublicAuthorityForm.fill)
              Ok(addPublicAuthorityPage(form, previous)).toFuture
            }
          }
        case _ => Redirect(routes.SubsidyController.getReportPayment()).toFuture
      }
    }
  }

  def postAddClaimPublicAuthority: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      getPrevious[SubsidyJourney](store).flatMap { previous =>
        claimPublicAuthorityForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addPublicAuthorityPage(errors, previous)).toFuture,
            form =>
              for {
                journey <- store.update[SubsidyJourney](_.setPublicAuthority(form))
                redirect <- journey.next
              } yield redirect
          )
      }
    }
  }

  def getAddClaimReference: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store.get[SubsidyJourney].flatMap {
        case Some(journey) =>
          if (!journey.isEligibleForStep) {
            Redirect(journey.previous).toFuture
          } else {
            val form = journey.traderRef.value.fold(claimTraderRefForm) { optionalTraderRef =>
              claimTraderRefForm.fill(OptionalTraderRef(optionalTraderRef.setValue, optionalTraderRef.value))
            }
            Ok(addTraderReferencePage(form, journey.previous)).toFuture
          }
        case _ => Redirect(routes.SubsidyController.getReportPayment()).toFuture
      }
    }
  }

  def postAddClaimReference: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
        claimTraderRefForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addTraderReferencePage(errors, previous)).toFuture,
            form =>
              for {
                updatedSubsidyJourney <- store.update[SubsidyJourney](_.setTraderRef(form))
                redirect <- updatedSubsidyJourney.next
              } yield redirect
          )
      }
    }
  }

  def getCheckAnswers: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      val result: OptionT[Future, Result] = for {
        journey <- store.get[SubsidyJourney].toContext
        _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
        claimDate <- journey.claimDate.value.toContext
        amount <- journey.claimAmount.value.toContext
        optionalEori <- journey.addClaimEori.value.toContext
        authority <- journey.publicAuthority.value.toContext
        optionalTraderRef <- journey.traderRef.value.toContext
        claimEori = optionalEori.value.map(EORI(_))
        traderRef = optionalTraderRef.value.map(TraderRef(_))
        previous = journey.previous
      } yield Ok(cyaPage(claimDate, amount, claimEori, authority, traderRef, previous))

      result.getOrElse(Redirect(routes.SubsidyController.getAddClaimReference()))
    }
  }

  def postCheckAnswers: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
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
          _ =
            if (journey.isAmend)
              auditService.sendEvent[NonCustomsSubsidyUpdated](
                AuditEvent.NonCustomsSubsidyUpdated(request.authorityId, ref, journey, currentDate)
              )
            else
              auditService.sendEvent[NonCustomsSubsidyAdded](
                AuditEvent.NonCustomsSubsidyAdded(request.authorityId, eori, ref, journey, currentDate)
              )
        } yield Redirect(routes.SubsidyController.getReportPayment())

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

  def getRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = withCDSAuthenticatedUser.async {
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

  def postRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = withCDSAuthenticatedUser.async {
    implicit request =>
      withLeadUndertaking { undertaking =>
        removeSubsidyClaimForm
          .bindFromRequest()
          .fold(
            formWithErrors => handleRemoveSubsidyFormError(formWithErrors, transactionId, undertaking),
            formValue =>
              if (formValue.value == "true") handleRemoveSubsidyValidAnswer(transactionId, undertaking)
              else Redirect(routes.SubsidyController.getReportPayment()).toFuture
          )
      }
  }

  def getChangeSubsidyClaim(transactionId: String): Action[AnyContent] = withCDSAuthenticatedUser.async {
    implicit request =>
      withLeadUndertaking { undertaking =>
        implicit val eori: EORI = request.eoriNumber
        val result = for {
          reference <- undertaking.reference.toContext
          nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
          subsidyJourney = SubsidyJourney.fromNonHmrcSubsidy(nonHmrcSubsidy)
          _ <- store.put(subsidyJourney).toContext
        } yield Redirect(routes.SubsidyController.getCheckAnswers())
        result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
      }
  }

  private def getNonHmrcSubsidy(
    transactionId: String,
    reference: UndertakingRef
  )(implicit r: AuthenticatedEscRequest[AnyContent]): OptionT[Future, NonHmrcSubsidy] =
    retrieveSubsidies(reference)
      .recoverWith({ case _ => Option.empty[UndertakingSubsidies].toFuture })
      .toContext
      .flatMap(_.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionId.contains(transactionId)).toContext)

  private def handleRemoveSubsidyFormError(
    formWithErrors: Form[FormValues],
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEscRequest[AnyContent]): Future[Result] = {
    val result = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
    } yield BadRequest(confirmRemovePage(formWithErrors, nonHmrcSubsidy))

    result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
  }

  private def handleRemoveSubsidyValidAnswer(
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEscRequest[AnyContent]): Future[Result] = {
    val result = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
      _ <- escService.removeSubsidy(reference, nonHmrcSubsidy).toContext
      _ = auditService.sendEvent[NonCustomsSubsidyRemoved](
        AuditEvent.NonCustomsSubsidyRemoved(request.authorityId, reference)
      )
    } yield Redirect(routes.SubsidyController.getReportPayment())

    result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
  }

  private def handleReportPaymentFormError(
    previous: String,
    undertaking: Undertaking,
    formWithErrors: Form[FormValues]
  )(implicit request: AuthenticatedEscRequest[AnyContent]): Future[Result] =
    retrieveSubsidies(undertaking.reference).map { subsidies =>
      val currentDate = timeProvider.today
      BadRequest(
        reportPaymentPage(
          formWithErrors,
          subsidies,
          undertaking,
          currentDate.toEarliestTaxYearStart,
          currentDate.toTaxYearEnd.minusYears(1),
          currentDate.toTaxYearStart,
          previous
        )
      )
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
              SubsidyAmount(journey.claimAmount.value.getOrElse(sys.error("Claim amount Missing"))),
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
