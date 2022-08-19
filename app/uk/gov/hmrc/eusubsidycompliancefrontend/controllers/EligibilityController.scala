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

import cats.implicits.catsSyntaxEq
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.{EscInitialActionBuilder, EscNoEnrolmentActionBuilders}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

// TODO - this controller needs a refactor - delegate journey choice to EligibilityJourney instead of the controller
@Singleton
class EligibilityController @Inject() (
                                        mcc: MessagesControllerComponents,
                                        auditService: AuditService,
                                        customsWaiversPage: CustomsWaiversPage,
                                        willYouClaimPage: WillYouClaimPage,
                                        notEligiblePage: NotEligiblePage,
                                        notEligibleToLeadPage: NotEligibleToLeadPage,
                                        checkEoriPage: CheckEoriPage,
                                        incorrectEoriPage: IncorrectEoriPage,
                                        escInitialActionBuilders: EscInitialActionBuilder,
                                        escNonEnrolmentActionBuilders: EscNoEnrolmentActionBuilders,
                                        escCDSActionBuilder: EscVerifiedEmailActionBuilders,
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
    println(s"Eligibility Controller - first empty page")
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { eligibilityJourney =>
        eligibilityJourney.firstEmpty.fold(Redirect(routes.UndertakingController.getUndertakingName()))(identity)
      }
  }

  def getCustomsWaivers: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    val form = customsWaiversForm
    Ok(customsWaiversPage(form)).toFuture
  }

  def postCustomsWaivers: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    customsWaiversForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(customsWaiversPage(errors)).toFuture,
        form => Redirect(getNext(form)).toFuture
      )
  }

  private def getNext(form: FormValues) =
    if (form.value == "true") routes.EligibilityController.getEoriCheck()
    else routes.EligibilityController.getWillYouClaim()

  def getWillYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    val form = willYouClaimForm
    Ok(willYouClaimPage(form, routes.EligibilityController.getCustomsWaivers().url)).toFuture
  }

  def postWillYouClaim: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    willYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(willYouClaimPage(errors, routes.EligibilityController.getCustomsWaivers().url)).toFuture,
        form =>
          if (form.value === "true") Redirect(routes.EligibilityController.getEoriCheck()).toFuture
          else Redirect(routes.EligibilityController.getNotEligible()).toFuture
      )

  }

  def getNotEligible: Action[AnyContent] = withNonVerfiedEmail.async { implicit request =>
    Ok(notEligiblePage()).toFuture
  }

  def getEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.getOrCreate[EligibilityJourney](EligibilityJourney()).map { journey =>
      val form = journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck =>
        eoriCheckForm.fill(FormValues(eoriCheck.toString))
      )
      Ok(checkEoriPage(form, eori, routes.EligibilityController.getCustomsWaivers().url))
    }
  }

  def postEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    eoriCheckForm
      .bindFromRequest()
      .fold(
        errors =>
          BadRequest(checkEoriPage(errors, eori, routes.EligibilityController.getCustomsWaivers().url)).toFuture,
        form =>
          store
            .update[EligibilityJourney](_.setEoriCheck(form.value.toBoolean))
            .flatMap { journey =>
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
