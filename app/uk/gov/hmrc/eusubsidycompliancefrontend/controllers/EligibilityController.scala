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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.TermsAndConditionsAccepted
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EligibilityJourney, JourneyTraverseService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

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
  escActionBuilders: EscActionBuilders,
  store: Store
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._

  def firstEmptyPage: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { eligibilityJourney =>
        eligibilityJourney.firstEmpty.fold(Redirect(routes.UndertakingController.getUndertakingName()))(identity)
      }
  }

  def getCustomsWaivers: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { journey =>
        val form = journey.customsWaivers.value.fold(customsWaiversForm)(customWaiverBool =>
          customsWaiversForm.fill(FormValues(customWaiverBool.toString))
        )
        Ok(customsWaiversPage(form))
      }
  }

  def postCustomsWaivers: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    customsWaiversForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(customsWaiversPage(errors))),
        form => store.update[EligibilityJourney](updateCustomWaiver(form.value.toBoolean)).flatMap(_.next)
      )
  }

  def getWillYouClaim: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[EligibilityJourney]
      .map(_.getOrElse(handleMissingSessionData("Eligibility Journey")))
      .map { journey =>
        val form = journey.willYouClaim.value.fold(willYouClaimForm)(willYouClaimBool =>
          willYouClaimForm.fill(FormValues(willYouClaimBool.toString))
        )
        Ok(willYouClaimPage(form, journey.previous))
      }
  }

  def postWillYouClaim: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].flatMap { previous =>
      willYouClaimForm
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(willYouClaimPage(errors, previous))),
          form => store.update[EligibilityJourney](updateWillYouClaim(form.value.toBoolean)).flatMap(_.next)
        )
    }
  }

  def getNotEligible: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    Ok(notEligiblePage()).toFuture
  }

  def getMainBusinessCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
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

  def postMainBusinessCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService
      .getPrevious[EligibilityJourney]
      .flatMap { previous =>
        mainBusinessCheckForm
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(mainBusinessCheckPage(errors, eori, previous))),
            form => store.update[EligibilityJourney](updateMainBusinessCheck(form.value.toBoolean)).flatMap(_.next)
          )
      }
  }

  def getNotEligibleToLead: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    Ok(notEligibleToLeadPage()).toFuture
  }

  def getTerms: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].map { previous =>
      Ok(termsPage(previous))
    }
  }

  def postTerms: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    termsForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store.update[EligibilityJourney](updateAcceptTerms(form.value.toBoolean)).flatMap { eligibilityJourney =>
            auditService.sendEvent(TermsAndConditionsAccepted(eori))
            eligibilityJourney.next
          }
      )

  }

  def getEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[EligibilityJourney].map {
      case Some(journey) =>
        val form =
          journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck => eoriCheckForm.fill(FormValues(eoriCheck.toString)))
        Ok(checkEoriPage(form, eori, journey.previous))
      case _ => handleMissingSessionData("Eligibility journey")
    }

  }

  def postEoriCheck: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].flatMap { previous =>
      eoriCheckForm
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(checkEoriPage(errors, eori, previous))),
          form => store.update[EligibilityJourney](updateEoriCheck(form.value.toBoolean)).flatMap(_.next)
        )
    }
  }

  def getIncorrectEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    Future.successful(Ok(incorrectEoriPage()))
  }

  def getCreateUndertaking: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[EligibilityJourney].map { previous =>
      Ok(createUndertakingPage(previous))
    }
  }

  def postCreateUndertaking: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    createUndertakingForm
      .bindFromRequest()
      .fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form =>
          store.update[EligibilityJourney](updateCreateUndertaking(form.value.toBoolean)).map { _ =>
            Redirect(routes.UndertakingController.getUndertakingName())
          }
      )
  }

  private def updateEligibilityJourney(eligibilityJourneyOpt: Option[EligibilityJourney])(
    f: EligibilityJourney => EligibilityJourney
  ) = eligibilityJourneyOpt.map(f)

  private def updateWillYouClaim(newWillYouClaim: Boolean)(eligibilityJourneyOpt: Option[EligibilityJourney]) =
    updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
      ej.copy(willYouClaim = ej.willYouClaim.copy(value = Some(newWillYouClaim)))
    }

  private def updateCustomWaiver(newCustomWaiver: Boolean)(eligibilityJourneyOpt: Option[EligibilityJourney]) =
    updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
      ej.copy(customsWaivers = ej.customsWaivers.copy(value = Some(newCustomWaiver)))
    }

  private def updateMainBusinessCheck(
    newMainBusinessCheck: Boolean
  )(eligibilityJourneyOpt: Option[EligibilityJourney]) = updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
    ej.copy(mainBusinessCheck = ej.mainBusinessCheck.copy(value = Some(newMainBusinessCheck)))
  }

  private def updateAcceptTerms(newAcceptTerms: Boolean)(eligibilityJourneyOpt: Option[EligibilityJourney]) =
    updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
      ej.copy(acceptTerms = ej.acceptTerms.copy(value = Some(newAcceptTerms)))
    }

  private def updateEoriCheck(newEoriCheck: Boolean)(eligibilityJourneyOpt: Option[EligibilityJourney]) =
    updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
      ej.copy(eoriCheck = ej.eoriCheck.copy(value = Some(newEoriCheck)))
    }

  private def updateCreateUndertaking(
    newCreateUndertaking: Boolean
  )(eligibilityJourneyOpt: Option[EligibilityJourney]) = updateEligibilityJourney(eligibilityJourneyOpt) { ej =>
    ej.copy(createUndertaking = ej.createUndertaking.copy(value = Some(newCreateUndertaking)))
  }

  lazy val customsWaiversForm: Form[FormValues] = Form(
    mapping("customswaivers" -> mandatory("customswaivers"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val mainBusinessCheckForm: Form[FormValues] = Form(
    mapping("mainbusinesscheck" -> mandatory("mainbusinesscheck"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val willYouClaimForm: Form[FormValues] = Form(
    mapping("willyouclaim" -> mandatory("willyouclaim"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val termsForm: Form[FormValues] = Form(
    mapping("terms" -> mandatory("terms"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val eoriCheckForm: Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val createUndertakingForm: Form[FormValues] = Form(
    mapping("createUndertaking" -> mandatory("createUndertaking"))(FormValues.apply)(FormValues.unapply)
  )

}
