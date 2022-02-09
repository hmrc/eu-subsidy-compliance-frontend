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

import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimDateFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, Store, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubsidyController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  reportPaymentPage: ReportPaymentPage,
  addClaimEoriPage: AddClaimEoriPage,
  addClaimAmountPage: AddClaimAmountPage,
  addClaimDatePage: AddClaimDatePage,
  addPublicAuthorityPage: AddPublicAuthorityPage,
  addTraderReferencePage: AddTraderReferencePage,
  cyaPage: ClaimCheckYourAnswerPage,
  confirmRemovePage: ConfirmRemoveClaim,
  claimDateFormProvider: ClaimDateFormProvider
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def getReportPayment: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
        journey <- store.get[SubsidyJourney]
        _ = if(journey.isEmpty) store.put(SubsidyJourney())
        undertaking <- store.get[Undertaking]
        reference = undertaking.getOrElse(throw new IllegalStateException("")).reference.getOrElse(throw new IllegalStateException(""))
        subsidies <- escService.retrieveSubsidy(SubsidyRetrieve(reference, None)).map(e => Some(e)).recoverWith({case _ => Future.successful(Option.empty[UndertakingSubsidies])})
      } yield (journey, subsidies, undertaking) match {
      case (Some(journey), subsidies, Some(undertaking)) => {
        journey
          .reportPayment
          .value
          .fold(
            Ok(reportPaymentPage(subsidies, undertaking)) // TODO populate subsidy list
          ) { x =>
            Ok(reportPaymentPage(subsidies, undertaking))
          }
      }
      case (None, None, Some(undertaking)) => // initialise the empty Journey model
        Ok(reportPaymentPage(None, undertaking))
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postReportPayment: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    reportPaymentForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        for {
          journey <- store.update[SubsidyJourney]({
            _.map { y =>
              y.copy(reportPayment = y.reportPayment.copy(value = Some(form.value.toBoolean)))
            }
          })
          redirect <- getJourneyNext(journey)
        } yield redirect
      }
    )
  }

  def getClaimAmount: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    //TODO add 'getPrevious to all'
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        journey
          .claimAmount
          .value
          .fold(
            Future.successful(
              Ok(addClaimAmountPage(claimAmountForm, journey.previous))
            )
          ) { x =>
            Future.successful(
              Ok(addClaimAmountPage(claimAmountForm.fill(x), journey.previous))
            )
          }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimAmount: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimAmountForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(addClaimAmountPage(formWithErrors, previous))),
        form => {
          for {
            journey <- store.update[SubsidyJourney]({ x =>
              x.map { y =>
              y.copy(claimAmount = y.claimAmount.copy(value = Some(form)))
              }
            })
            redirect <- getJourneyNext(journey)
          } yield redirect
        }
      )
    }
  }

  def getClaimDate: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        journey
          .claimDate
          .value
          .fold(
            Future.successful(
              Ok(addClaimDatePage(
                claimDateForm, 
                journey.previous
              ))
            )
          ) { x =>
            Future.successful(
              Ok(addClaimDatePage(
                claimDateForm.fill(x),
                journey.previous
              ))
            )
          }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postClaimDate: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimDateForm.bindFromRequest().fold(
        formWithErrors => Future(BadRequest(addClaimDatePage(formWithErrors, previous))),
        form => {
          for {
            journey <- store.update[SubsidyJourney]({ x =>
              x.map { y =>
              y.copy(claimDate = y.claimDate.copy(value = Some(form)))
            }
            })
            redirect <- getJourneyNext(journey)
          } yield redirect
        }
      )
    }
  }

  def getAddClaimEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        journey
          .addClaimEori
          .value
          .fold(
            Future.successful(
              Ok(addClaimEoriPage(claimEoriForm, journey.previous))
            )
          ) { x =>
            val a = x.getOrElse("")
            Future.successful(
              Ok(addClaimEoriPage(claimEoriForm.fill(OptionalEORI(a,x)), journey.previous))
            )
          }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimEoriForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(addClaimEoriPage(formWithErrors, previous))),
        form => {
          for {
            journey <- store.update[SubsidyJourney]({
                _.map { journey =>
                  journey.copy(addClaimEori = journey.addClaimEori.copy(value = Some(form.value.map(EORI(_)))))
                }
              })
            redirect <- getJourneyNext(journey)
          } yield redirect
        }
      )
    }
  }

  def getAddClaimPublicAuthority: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) =>
        journey
          .publicAuthority
          .value
          .fold(
            Future.successful(
              Ok(addPublicAuthorityPage(claimPublicAuthorityForm, journey.previous))
            )
          ) { x =>
            Future.successful(
              Ok(addPublicAuthorityPage(claimPublicAuthorityForm.fill(x), journey.previous))
            )
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
            journey <- store.update[SubsidyJourney]({ x =>
                  x.map { y =>
                y.copy(publicAuthority = y.publicAuthority.copy(value = Some(form)))
              }
              })
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
        journey
          .traderRef
          .value
          .fold(
            Future.successful(
              Ok(addTraderReferencePage(claimTraderRefForm, journey.previous))
            )
          ) { x =>
            val a = x.getOrElse("")
            Future.successful(
              Ok(addTraderReferencePage(claimTraderRefForm.fill(OptionalTraderRef(a,x)), journey.previous))
            )
          }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }

  def postAddClaimReference: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[SubsidyJourney](store).flatMap { previous =>
      claimTraderRefForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(addTraderReferencePage(errors, previous))),
        form => {
          for {
            journey <- store.update[SubsidyJourney]({ x =>
            x.map { y =>
            y.copy(traderRef = y.traderRef.copy(value = Some(form.value.map(TraderRef(_)))))
          }
          })
            redirect <- getJourneyNext(journey)

          } yield redirect
        }
      )
    }
  }

  def getCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) => {
        Future.successful(
          Ok(
            cyaPage(
              journey.claimDate.value.getOrElse(throw new IllegalStateException("Claim date should be defined")),
              journey.claimAmount.value.getOrElse(throw new IllegalStateException("Claim amount payment should be defined")),
              journey.addClaimEori.value.getOrElse(throw new IllegalStateException("Claim EORI payment should be defined")),
              journey.publicAuthority.value.getOrElse(throw new IllegalStateException("Public Authority payment should be defined")),
              journey.traderRef.value.getOrElse(throw new IllegalStateException("Trader Reference payment should be defined")),
              journey.previous
            )
          )
        )
      }
      case _ => handleMissingSessionData("Subsidy journey")
    }
  }
  def postCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[SubsidyJourney]({ x =>
          x.map { y =>
            y.copy(cya = y.cya.copy(value = Some(form.value.toBoolean)))
          }
        })
          .flatMap { journey: SubsidyJourney =>
            journey.publicAuthority.value.getOrElse(handleMissingSessionData("publicAuthority"))
            journey.traderRef.value.getOrElse(handleMissingSessionData("trader ref"))
            journey.claimAmount.value.getOrElse(handleMissingSessionData("claimAmount"))
            journey.addClaimEori.value.getOrElse(handleMissingSessionData("addClaimEori"))
            for {
              underTaking <- store.get[Undertaking]
              ref = underTaking.getOrElse(throw new IllegalStateException("")).reference.getOrElse(throw new IllegalStateException(""))
              _ <- escService.createSubsidy(ref, journey)
              _ <- store.put(SubsidyJourney())
            } yield {
              Redirect(routes.SubsidyController.getReportPayment())
            }
          }
      }
    )
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
        BadRequest(confirmRemovePage(formWithErrors, sub)),
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

  private def getJourneyNext(journey: SubsidyJourney)(implicit request: Request[_]) =
    if(journey.isAmend()) Future.successful(Redirect(routes.SubsidyController.getCheckAnswers())) else journey.next

  lazy val reportPaymentForm: Form[FormValues] = Form(
    mapping("reportPayment" -> mandatory("reportPayment"))(FormValues.apply)(FormValues.unapply))

  // TODO validate the EORI matches regex
  val claimEoriForm: Form[OptionalEORI] = Form(
    mapping(
      "should-claim-eori" -> mandatory("should-claim-eori"),
      "claim-eori" -> optional(text)
    )((a,b) => OptionalEORI(a, if(b.nonEmpty) Some(s"GB${b.get}") else b)
    )(a => Some((a.setValue, a.value.fold(Option.empty[String])(e => Some(e.drop(2))))))
      .transform[OptionalEORI](
      a => if (a.setValue == "false") a.copy(value = None) else a,
      b => b
    ).verifying(
      "error.format", a => a.setValue == "false" || a.value.fold(false)(entered => s"GB${entered.drop(2)}".matches(EORI.regex))
    )
  )

  val claimTraderRefForm: Form[OptionalTraderRef] = Form(
    mapping(
      "should-store-trader-ref" -> mandatory("should-claim-eori"),
      "claim-trader-ref" -> optional(text)
    )(OptionalTraderRef.apply)(OptionalTraderRef.unapply)
      .transform[OptionalTraderRef](
      a => if (a.setValue == "false") a.copy(value = None) else a,
      b => b
    )
  )

  lazy val claimPublicAuthorityForm: Form[String] = Form(
    "claim-public-authority" -> mandatory("claim-public-authority")
  )

  lazy val claimAmountForm : Form[BigDecimal] = Form(
    mapping("claim-amount" -> bigDecimal
      .verifying("error.amount.incorrectFormat", e => e.scale == 2 || e.scale == 0)
      .verifying("error.amount.tooBig", e => e.toString().length < 17)
      .verifying("error.amount.tooSmall", e => e > 0.01)
  )
    (identity)(Some(_)))

  private val claimDateForm = claimDateFormProvider.form

  lazy val removeSubsidyClaimForm: Form[FormValues] = Form(
    mapping("removeSubsidyClaim" -> mandatory("removeSubsidyClaim"))(FormValues.apply)(FormValues.unapply))

  lazy val cyaForm: Form[FormValues] = Form(
    mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

}
