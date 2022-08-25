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
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.{EscInitialActionBuilder, EscNoEnrolmentActionBuilders}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms.{DoYouClaimFormPage, WillYouClaimFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EligibilityController @Inject() (
                                        mcc: MessagesControllerComponents,
                                        auditService: AuditService,
                                        doYouClaimPage: DoYouClaimPage,
                                        willYouClaimPage: WillYouClaimPage,
                                        notEligiblePage: NotEligiblePage,
                                        notEligibleToLeadPage: NotEligibleToLeadPage,
                                        checkEoriPage: CheckEoriPage,
                                        incorrectEoriPage: IncorrectEoriPage,
                                        escInitialActionBuilders: EscInitialActionBuilder,
                                        escNonEnrolmentActionBuilders: EscNoEnrolmentActionBuilders,
                                        emailService: EmailService,
                                        override val store: Store
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with FormHelpers {

  import escInitialActionBuilders._
  import escNonEnrolmentActionBuilders._

  private val customsWaiversForm: Form[FormValues] = Form(
    mapping("customswaivers" -> mandatory("customswaivers"))(FormValues.apply)(FormValues.unapply)
  )

  private val willYouClaimForm: Form[FormValues] = Form(
    mapping("willyouclaim" -> mandatory("willyouclaim"))(FormValues.apply)(FormValues.unapply)
  )

  private val eoriCheckForm: Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply)
  )

  def firstEmptyPage: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { eligibilityJourney =>
        eligibilityJourney.firstEmpty.fold(Redirect(routes.UndertakingController.getUndertakingName()))(identity)
      }
  }

  def getDoYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    val form = customsWaiversForm
    Ok(doYouClaimPage(form)).toFuture
  }

  def postDoYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    customsWaiversForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(doYouClaimPage(errors)).toFuture,
        form => {
          EligibilityJourney(
            doYouClaim = DoYouClaimFormPage(form.value.toBoolean.some)
          ).next
        }
      )
  }

  def getWillYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    Ok(willYouClaimPage(willYouClaimForm, routes.EligibilityController.getDoYouClaim().url)).toFuture
  }

  def postWillYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    willYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(willYouClaimPage(errors, routes.EligibilityController.getDoYouClaim().url)).toFuture,
        form => {
          // Set up representative state in EligiblityJourney before we call next.
          val journey = EligibilityJourney(
            // We are on the will you claim page so the user must have entered false on the do you claim form
            doYouClaim = DoYouClaimFormPage(false.some),
            willYouClaim = WillYouClaimFormPage(form.value.toBoolean.some)
          )

          journey.next
        }
      )

  }

  def getNotEligible: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    Ok(notEligiblePage()).toFuture
  }

  // TODO - consider factoring out state determination?
  def getEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val ref = request.headers.get("Referer")
    val eligibilityJourney = EligibilityJourney(
      // Reconstruct do you / will you claim answers from referring page
      doYouClaim = DoYouClaimFormPage(ref.map(_.endsWith(routes.EligibilityController.getDoYouClaim().url))),
      willYouClaim = WillYouClaimFormPage(ref.map(_.endsWith(routes.EligibilityController.getWillYouClaim().url))),
    )
    store.getOrCreate[EligibilityJourney](eligibilityJourney).map { journey =>
      val form = journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck =>
        eoriCheckForm.fill(FormValues(eoriCheck.toString))
      )
      Ok(checkEoriPage(form, eori, routes.EligibilityController.getDoYouClaim().url))
    }
  }

  def postEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    eoriCheckForm
      .bindFromRequest()
      .fold(
        errors =>
          BadRequest(checkEoriPage(errors, eori, routes.EligibilityController.getDoYouClaim().url)).toFuture,
        form =>
          store
            .update[EligibilityJourney](_.setEoriCheck(form.value.toBoolean))
            .flatMap { journey =>
              // TODO - here we can check if the undertaking exists and direct the user to the account home page
              if (journey.isComplete) Redirect(routes.UndertakingController.getUndertakingName()).toFuture
              else journey.next
            }
      )
  }

  def getNotEligibleToLead: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    Ok(notEligibleToLeadPage()).toFuture
  }

  def getIncorrectEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    Ok(incorrectEoriPage()).toFuture
  }

}
