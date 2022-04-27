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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.{EscCDSActionBuilders, EscNoEnrolmentActionBuilders}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.TermsAndConditionsAccepted
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EligibilityJourney, JourneyTraverseService, RetrieveEmailService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.ExecutionContext

@Singleton
class EligibilityController @Inject() (
  mcc: MessagesControllerComponents,
  auditService: AuditService,
  journeyTraverseService: JourneyTraverseService,
  customsWaiversPage: CustomsWaiversPage,
  willYouClaimPage: WillYouClaimPage,
  notEligiblePage: NotEligiblePage,
  mainBusinessCheckPage: MainBusinessCheckPage,
  notEligibleToLeadPage: NotEligibleToLeadPage,
  termsPage: EligibilityTermsAndConditionsPage,
  checkEoriPage: CheckEoriPage,
  incorrectEoriPage: IncorrectEoriPage,
  createUndertakingPage: CreateUndertakingPage,
  escNonEnrolmentActionBuilders: EscNoEnrolmentActionBuilders,
  escCDSActionBuilder: EscCDSActionBuilders,
  retrieveEmailService: RetrieveEmailService,
  store: Store
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escNonEnrolmentActionBuilders._
  import escCDSActionBuilder._

  private val customsWaiversForm: Form[FormValues] = Form(
    mapping("customswaivers" -> mandatory("customswaivers"))(FormValues.apply)(FormValues.unapply)
  )

  private val mainBusinessCheckForm: Form[FormValues] = Form(
    mapping("mainbusinesscheck" -> mandatory("mainbusinesscheck"))(FormValues.apply)(FormValues.unapply)
  )

  private val willYouClaimForm: Form[FormValues] = Form(
    mapping("willyouclaim" -> mandatory("willyouclaim"))(FormValues.apply)(FormValues.unapply)
  )

  private val termsForm: Form[FormValues] = Form(
    mapping("terms" -> mandatory("terms"))(FormValues.apply)(FormValues.unapply)
  )

  private val eoriCheckForm: Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply)
  )

  private val createUndertakingForm: Form[FormValues] = Form(
    mapping("createUndertaking" -> mandatory("createUndertaking"))(FormValues.apply)(FormValues.unapply)
  )

  def firstEmptyPage: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { eligibilityJourney =>
        eligibilityJourney.firstEmpty.fold(Redirect(routes.UndertakingController.getUndertakingName()))(identity)
      }
  }

  def getCustomsWaivers: Action[AnyContent] = withNonAuthenticatedUser.async { implicit request =>
    val form = customsWaiversForm
    Ok(customsWaiversPage(form)).toFuture
  }

  def postCustomsWaivers: Action[AnyContent] = withNonAuthenticatedUser.async { implicit request =>
    customsWaiversForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(customsWaiversPage(errors)).toFuture,
        form => Redirect(getNext(form)).toFuture
      )
  }

  private def getNext(form: FormValues) = if (form.value == "true") routes.EligibilityController.getEoriCheck()
  else routes.EligibilityController.getWillYouClaim()

  def getWillYouClaim: Action[AnyContent] = withNonAuthenticatedUser.async { implicit request =>
    val form = willYouClaimForm
    Ok(willYouClaimPage(form, routes.EligibilityController.getCustomsWaivers().url)).toFuture
  }

  def postWillYouClaim: Action[AnyContent] = withNonAuthenticatedUser.async { implicit request =>
    willYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(willYouClaimPage(errors, routes.EligibilityController.getCustomsWaivers().url)).toFuture,
        form =>
          if (form.value === "true") Redirect(routes.EligibilityController.getEoriCheck()).toFuture
          else Redirect(routes.EligibilityController.getNotEligible()).toFuture
      )

  }

  def getEoriCheck: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    retrieveEmailService.retrieveEmailByEORI(eori) flatMap {
      _.emailType match {
        case EmailType.VerifiedEmail =>
          store.get[EligibilityJourney].map {
            case Some(journey) =>
              val form =
                journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck =>
                  eoriCheckForm.fill(FormValues(eoriCheck.toString))
                )
              Ok(checkEoriPage(form, eori, journey.previous))
            case _ => handleMissingSessionData("Eligibility journey")
          }
        case EmailType.UnVerifiedEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUnverifiedEmailAddress()).toFuture
        case EmailType.UnDeliverableEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUndeliveredEmailAddress()).toFuture
      }
    }

  }

  def postEoriCheck: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService
      .getPrevious[EligibilityJourney]
      .flatMap { previous =>
        eoriCheckForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(checkEoriPage(errors, eori, previous)).toFuture,
            form => store.update[EligibilityJourney](_.setEoriCheck(form.value.toBoolean)).flatMap(_.next)
          )
      }

  }

  def getNotEligible: Action[AnyContent] = withNonAuthenticatedUser.async { implicit request =>
    Ok(notEligiblePage()).toFuture
  }

  def getMainBusinessCheck: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { journey =>
        val form = journey.mainBusinessCheck.value.fold(mainBusinessCheckForm)(mainBC =>
          mainBusinessCheckForm.fill(FormValues(mainBC.toString))
        )
        Ok(mainBusinessCheckPage(form, eori, journey.previous))
      }
  }

  def postMainBusinessCheck: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService
      .getPrevious[EligibilityJourney]
      .flatMap { previous =>
        mainBusinessCheckForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(mainBusinessCheckPage(errors, eori, previous)).toFuture,
            form => store.update[EligibilityJourney](_.setMainBusinessCheck(form.value.toBoolean)).flatMap(_.next)
          )
      }
  }

  def getNotEligibleToLead: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    Ok(notEligibleToLeadPage()).toFuture
  }

  def getTerms: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].map { previous =>
      Ok(termsPage(previous, eori))
    }
  }

  def postTerms: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    termsForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store.update[EligibilityJourney](_.setAcceptTerms(form.value.toBoolean)).flatMap { eligibilityJourney =>
            auditService.sendEvent(TermsAndConditionsAccepted(eori))
            eligibilityJourney.next
          }
      )
  }

  def getIncorrectEori: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    Ok(incorrectEoriPage()).toFuture
  }

  def getCreateUndertaking: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].map { previous =>
      Ok(createUndertakingPage(previous, eori))
    }
  }

  def postCreateUndertaking: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    createUndertakingForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store.update[EligibilityJourney](_.setCreateUndertaking(form.value.toBoolean)).map { _ =>
            Redirect(routes.UndertakingController.getUndertakingName())
          }
      )
  }

}
