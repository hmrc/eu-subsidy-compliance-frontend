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
import play.api.data.Forms.{bigDecimal, mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimDateFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubsidyController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  reportPaymentPage: ReportPaymentPage,
  addClaimEoriPage: AddClaimEoriPage,
  addClaimAmountPage: AddClaimAmountPage,
  addClaimDatePage: AddClaimDatePage,
  addPublicAuthorityPage: AddPublicAuthorityPage,
  addTraderReferencePage: AddTraderReferencePage,
  cyaPage: ClaimCheckYourAnswerPage,
  confirmRemovePage: ConfirmRemoveClaim,
  claimDateFormProvider: ClaimDateFormProvider,
  timeProvider: TimeProvider
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def getReportPayment: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val result = for {
      _           <- store.get[SubsidyJourney].toContext.orElse(store.put(SubsidyJourney()).toContext)
      undertaking <- store.get[Undertaking].toContext
      reference   <- undertaking.reference.toContext
    } yield (reference, undertaking)

    result.foldF(handleMissingSessionData("Subsidy journey")) {
      case (reference, undertaking) =>
        retrieveSubsidiesOrNone(reference).map { subsidies =>
          Ok(reportPaymentPage(
            subsidies,
            undertaking,
            timeProvider.today.toEarliestTaxYearStart,
            timeProvider.today.toTaxYearEnd.minusYears(1),
            timeProvider.today.toTaxYearStart,
          ))
        }
    }
  }

  private def retrieveSubsidiesOrNone(r: UndertakingRef)(implicit hc: HeaderCarrier) =
    escService
      .retrieveSubsidy(SubsidyRetrieve(r, None))
      .map(Option(_))
      .fallbackTo(Option.empty.toFuture)

  def postReportPayment: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    reportPaymentForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("report payment form submission failed"),
      (form: FormValues) => for {
        journey <- store.update[SubsidyJourney](updateReportPayment(form))
        redirect <- getJourneyNext(journey)
      } yield redirect
    )
  }

  def getClaimAmount: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val result: OptionT[Future, Result] = for {
      subsidyJourney <- store.get[SubsidyJourney].toContext
      addClaimDate <- subsidyJourney.claimDate.value.toContext
      previous = subsidyJourney.previous
    } yield {
      val form = subsidyJourney.claimAmount.value.fold(claimAmountForm)(claimAmountForm.fill)
      Ok(addClaimAmountPage(form, previous, addClaimDate.year, addClaimDate.month))
    }
    result.getOrElse(handleMissingSessionData(" Subsidy journey"))
  }

  def postAddClaimAmount: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmit(previous: Journey.Uri, addClaimDate: DateFormValues) = {
      claimAmountForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(addClaimAmountPage(formWithErrors, previous, addClaimDate.year, addClaimDate.month))),
        form => for {
          journey <- store.update[SubsidyJourney](updateClaimAmount(form))
          redirect <- getJourneyNext(journey)
        } yield redirect
      )
    }

    val result: OptionT[Future, Result] = for {
      subsidyJourney <- store.get[SubsidyJourney].toContext
      addClaimDate <- subsidyJourney.claimDate.value.toContext
      previous = subsidyJourney.previous
      resultFormSubmit <- handleFormSubmit(previous, addClaimDate).toContext
    } yield (resultFormSubmit)
    result.getOrElse(handleMissingSessionData(" Subsidy journey"))
  }

  def getClaimDate: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
        case Some(journey) => journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
          val form = journey.claimDate.value.fold(claimDateForm)(claimDateForm.fill)
          Ok(addClaimDatePage(form, previous)).toFuture
        }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postClaimDate: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimDateForm.bindFromRequest().fold(
        formWithErrors => BadRequest(addClaimDatePage(formWithErrors, previous)).toFuture,
        form => for {
          journey <- store.update[SubsidyJourney](updateClaimDate(form))
          redirect <- getJourneyNext(journey)
        } yield redirect)
      }
    }

  def getAddClaimEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
          val form = journey.addClaimEori.value.fold(claimEoriForm) { optionalEORI =>
            claimEoriForm.fill(OptionalEORI(optionalEORI.setValue, optionalEORI.value))
          }
          Ok(addClaimEoriPage(form, previous)).toFuture
        }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
      claimEoriForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(addClaimEoriPage(formWithErrors, previous))),
        (form: OptionalEORI) => {
          for {
            journey <- store.update[SubsidyJourney](updateClaimEori(form))
            redirect <- getJourneyNext(journey)
          } yield redirect
        }
      )
    }
  }

  def getAddClaimPublicAuthority: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) => journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
        val form = journey.publicAuthority.value.fold(claimPublicAuthorityForm)(claimPublicAuthorityForm.fill)
        Ok(addPublicAuthorityPage(form, previous)).toFuture
      }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimPublicAuthority: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimPublicAuthorityForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(addPublicAuthorityPage(errors, previous))),
        form => {
          for {
            journey <- store.update[SubsidyJourney](updateClaimAuthority(form))
            redirect <- getJourneyNext(journey)
          } yield redirect
        }
      )
    }
  }

  def getAddClaimReference: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        val form = journey.traderRef.value.fold(claimTraderRefForm) { optionalTraderRef =>
          claimTraderRefForm.fill(OptionalTraderRef(optionalTraderRef.setValue, optionalTraderRef.value))
        }
        Ok(addTraderReferencePage(form, journey.previous)).toFuture
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimReference: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[SubsidyJourney].flatMap { previous =>
      claimTraderRefForm.bindFromRequest().fold(
        errors => BadRequest(addTraderReferencePage(errors, previous)).toFuture,
        form => for {
          updatedSubsidyJourney <- store.update[SubsidyJourney](updateOptionalTraderRef(form))
          redirect <- getJourneyNext(updatedSubsidyJourney)
        } yield redirect
      )
    }
  }

  def getCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val result: OptionT[Future, Result] = for {
      journey           <- store.get[SubsidyJourney].toContext
      _                 <- validateSubsidyJourneyFieldsPopulated(journey).toContext
      claimDate         <- journey.claimDate.value.toContext
      amount            <- journey.claimAmount.value.toContext
      optionalEori      <- journey.addClaimEori.value.toContext
      authority         <- journey.publicAuthority.value.toContext
      optionalTraderRef <- journey.traderRef.value.toContext
      claimEori = optionalEori.value.map(EORI(_))
      traderRef = optionalTraderRef.value.map(TraderRef(_))
      previous  = journey.previous
    } yield Ok(cyaPage(claimDate, amount, claimEori, authority, traderRef, previous))

    result.getOrElse(handleMissingSessionData("Subsidy journey"))
  }

  def postCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    cyaForm.bindFromRequest().fold(
      _ => sys.error("Error processing subsidy cya form submission"),
      form => {
        for {
          journey <- store.update[SubsidyJourney] (updateCya(form)).map(Option(_)).toContext
          _ <- validateSubsidyJourneyFieldsPopulated(journey).toContext
          undertaking <- store.get[Undertaking].toContext.orElseF(handleMissingSessionData("undertaking"))
          ref <- undertaking.reference.toContext
          _ <- escService.createSubsidy(ref, journey).toContext
          _ <- store.put(SubsidyJourney()).toContext
        } yield {
          Redirect(routes.SubsidyController.getReportPayment())
        }
      }.getOrElse(sys.error("Error processing subsidy cya form submission"))
    )
  }

  private def validateSubsidyJourneyFieldsPopulated(journey: SubsidyJourney): Option[Unit] = {
    for {
      _ <- journey.addClaimEori.value.orElse(handleMissingSessionData("claim EORI"))
      _ <- journey.claimAmount.value.orElse(handleMissingSessionData("claim mount"))
      _ <- journey.claimDate.value.orElse(handleMissingSessionData("claim date"))
      _ <- journey.publicAuthority.value.orElse(handleMissingSessionData("public authority"))
      _ <- journey.traderRef.value.orElse(handleMissingSessionData("trader ref"))
    } yield ()
  }

  def getRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
      undertaking <- store.get[Undertaking]
      reference = undertaking.getOrElse(throw new IllegalStateException("")).reference.getOrElse(throw new IllegalStateException(""))
      subsidies <- escService.retrieveSubsidy(SubsidyRetrieve(reference, None)).map(e => Some(e)).recoverWith({case _ => Future.successful(Option.empty[UndertakingSubsidies])})
      sub = subsidies.get.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionID.contains(transactionId)).get
    } yield {
      Ok(confirmRemovePage(removeSubsidyClaimForm, sub))
    }
  }

  def postRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    removeSubsidyClaimForm.bindFromRequest().fold(formWithErrors =>
      for {
        undertaking <- store.get[Undertaking]
        reference = undertaking.getOrElse(throw new IllegalStateException("")).reference.getOrElse(throw new IllegalStateException(""))
        subsidies <- escService.retrieveSubsidy(SubsidyRetrieve(reference, None)).map(e => Some(e)).recoverWith({case _ => Future.successful(Option.empty[UndertakingSubsidies])})
        sub = subsidies.get.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionID.contains(transactionId)).get
      } yield {
        BadRequest(confirmRemovePage(formWithErrors, sub))
      }, formValue => {
      if(formValue.value == "true")
        for {
          undertaking <- store.get[Undertaking]
          reference = undertaking.getOrElse(throw new IllegalStateException("")).reference.getOrElse(throw new IllegalStateException(""))
          subsidies <- escService.retrieveSubsidy(SubsidyRetrieve(reference, None)).map(e => Some(e)).recoverWith({ case _ => Future.successful(Option.empty[UndertakingSubsidies]) })
          sub = subsidies.get.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionID.contains(transactionId)).get
          _ <- escService.removeSubsidy(reference, sub)
        } yield {
          Redirect(routes.SubsidyController.getReportPayment())
        }
      else {
        Future(Redirect(routes.SubsidyController.getReportPayment()))
      }
    }
    )
  }

  def getChangeSubsidyClaim(transactionId: String): Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
      undertaking <- store.get[Undertaking]
      reference = undertaking.flatMap(_.reference).getOrElse(handleMissingSessionData("Reference"))
      subsidies <- escService.retrieveSubsidy(SubsidyRetrieve(reference, None)).map(e => Some(e)).recoverWith({case _ => Future.successful(Option.empty[UndertakingSubsidies])})
      sub = subsidies.get.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionID.contains(transactionId)).get
      _ = store.put(
        SubsidyJourney.fromNonHmrcSubsidy(sub)
      )
    } yield {
      Redirect(routes.SubsidyController.getCheckAnswers())
    }
  }

  private def updateSubsidyJourney(os: Option[SubsidyJourney])(f: SubsidyJourney => SubsidyJourney) = os.map(f)

  private def updateReportPayment(f: FormValues)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(reportPayment = s.reportPayment.copy(value = Some(f.value.toBoolean)))
  }

  private def updateClaimAmount(b: BigDecimal)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(claimAmount = s.claimAmount.copy(value = Some(b)))
  }

  private def updateClaimDate(d: DateFormValues)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(claimDate = s.claimDate.copy(value = Some(d)))
  }

  private def updateClaimEori(oe: OptionalEORI)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(addClaimEori = s.addClaimEori.copy(value = oe.some))
  }

  private def updateClaimAuthority(pa: String)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(publicAuthority = s.publicAuthority.copy(value = pa.some))
  }

  private def updateOptionalTraderRef(otr: OptionalTraderRef)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    val updatedTraderRef = s.traderRef.copy(value = OptionalTraderRef(otr.setValue, otr.value).some)
    s.copy(traderRef = updatedTraderRef)
  }

  private def updateCya(f: FormValues)(os: Option[SubsidyJourney]) = updateSubsidyJourney(os) { s =>
    s.copy(cya = s.cya.copy(value = f.value.toBoolean.some))
  }

  // TODO - move this into SubsidyJourney (see UndertakingJourney.next)
  private def getJourneyNext(journey: SubsidyJourney)(implicit request: Request[_]) =
    if(journey.isAmend) Future.successful(Redirect(routes.SubsidyController.getCheckAnswers()))
    else journey.next

  private val reportPaymentForm: Form[FormValues] = Form(
    mapping("reportPayment" -> mandatory("reportPayment"))(FormValues.apply)(FormValues.unapply))

  // TODO validate the EORI matches regex
  private val claimEoriForm: Form[OptionalEORI] = Form(
    mapping(
      "should-claim-eori" -> mandatory("should-claim-eori"),
      "claim-eori" -> optional(text)
    )((radioSelected, eori) => claimEoriFormApply(radioSelected, eori)
    )(optionalEORI => Some((optionalEORI.setValue, optionalEORI.value.fold(Option.empty[String])(e => Some(e.drop(2))))))
      .transform[OptionalEORI](
        optionalEORI => if (optionalEORI.setValue == "false") optionalEORI.copy(value = None) else optionalEORI,
        identity
    ).verifying(
      "error.format", a => a.setValue == "false" || a.value.fold(false)(entered => s"GB${entered.drop(2)}".matches(EORI.regex))
    )
  )

  private def claimEoriFormApply(input: String, eoriOpt: Option[String]) =
    (input, eoriOpt) match {
      case (radioSelected, Some(eori)) => OptionalEORI(radioSelected, Some(s"GB$eori"))
      case (radioSelected, other) => OptionalEORI(radioSelected, other)
    }

  private val claimTraderRefForm: Form[OptionalTraderRef] = Form(
    mapping(
      "should-store-trader-ref" -> mandatory("should-store-trader-ref"),
      "claim-trader-ref" -> optional(text)
    )(OptionalTraderRef.apply)(OptionalTraderRef.unapply)
    .transform[OptionalTraderRef](
      optionalTraderRef => if (optionalTraderRef.setValue == "false") optionalTraderRef.copy(value = None) else optionalTraderRef,
      identity
    ).verifying(
      "error.isempty", optionalTraderRef => optionalTraderRef.setValue == "false" || optionalTraderRef.value.nonEmpty
    )
  )

  private val claimPublicAuthorityForm: Form[String] = Form(
    "claim-public-authority" -> mandatory("claim-public-authority")
  )

  private val claimAmountForm : Form[BigDecimal] = Form(
    mapping("claim-amount" -> bigDecimal
      .verifying("error.amount.tooBig", e => e.toString().length < 17)
      .verifying("error.amount.incorrectFormat", e => e.scale == 2 || e.scale == 0)
      .verifying("error.amount.tooSmall", e => e > 0.01)
  )
    (identity)(Some(_)))

  private val claimDateForm = claimDateFormProvider.form

  private val removeSubsidyClaimForm: Form[FormValues] = Form(
    mapping("removeSubsidyClaim" -> mandatory("removeSubsidyClaim"))(FormValues.apply)(FormValues.unapply))

  private val cyaForm: Form[FormValues] = Form(
    mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

}
