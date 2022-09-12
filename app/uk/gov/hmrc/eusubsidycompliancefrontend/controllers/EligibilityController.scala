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

import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.RequestSyntax.RequestOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
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
                                        checkEoriPage: CheckEoriPage,
                                        incorrectEoriPage: IncorrectEoriPage,
                                        actionBuilders: ActionBuilders,
                                        emailService: EmailService,
                                        escService: EscService,
                                        override val store: Store
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with FormHelpers {

  import actionBuilders._

  private val doYouClaimUrl = routes.EligibilityController.getDoYouClaim().url
  private val willYouClaimUrl = routes.EligibilityController.getWillYouClaim().url

  private val doYouClaimForm = formWithSingleMandatoryField("customswaivers")
  private val willYouClaimForm = formWithSingleMandatoryField("willyouclaim")
  private val eoriCheckForm = formWithSingleMandatoryField("eoricheck")

  def firstEmptyPage: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[EligibilityJourney].toContext
      .map(_.firstEmpty.getOrElse(Redirect(routes.UndertakingController.firstEmptyPage())))
      .getOrElse {
        // If we get here it must be the first time we've hit the service with an enrolment since there is nothing
        // in the journey store. In this case we route the user to the eoriCheck page.
        Redirect(routes.EligibilityController.getEoriCheck())
      }
  }

  def getDoYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    Ok(doYouClaimPage(doYouClaimForm)).toFuture
  }

  def postDoYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    doYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(doYouClaimPage(errors)).toFuture,
        form => {
          if (form.value.isTrue) checkEnrolment
          else EligibilityJourney()
            .withDoYouClaim(false)
            .next
        }
      )
  }

  def getWillYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    Ok(willYouClaimPage(willYouClaimForm, routes.EligibilityController.getDoYouClaim().url)).toFuture
  }

  def postWillYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    willYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(willYouClaimPage(errors, routes.EligibilityController.getDoYouClaim().url)).toFuture,
        form => {
          if (form.value.isTrue) checkEnrolment
          else
            // Set up representative state in EligibilityJourney before we call next.
            EligibilityJourney()
              // We are on the will you claim page so the user must have entered false on the do you claim form
              .withDoYouClaim(false)
              .withWillYouClaim(false)
              .next
        }
      )

  }

  def getNotEligible: Action[AnyContent] = notEnrolled.async { implicit request =>
    Ok(notEligiblePage()).toFuture
  }

  private def checkEnrolment(implicit request: AuthenticatedRequest[AnyContent]) =
    if (request.isFrom(doYouClaimUrl) || request.isFrom(willYouClaimUrl))
      Redirect(appConfig.eccEscSubscribeUrl).toFuture
    else
      Redirect(routes.EligibilityController.getEoriCheck().url).toFuture

  def getEoriCheck: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def renderPage = {
      // At this stage we have an enrolment so the user must be eligible to use the service.
      val eligibilityJourney = EligibilityJourney()
        .withWillYouClaim(true)

      store.getOrCreate[EligibilityJourney](eligibilityJourney).map { journey =>

        val form = journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck =>
          eoriCheckForm.fill(FormValues(eoriCheck.toString))
        )

        val backLink = request.headers
          .get("Referer")
          .getOrElse(doYouClaimUrl)

        Ok(checkEoriPage(form, eori, backLink))
      }
    }

    // Check if the undertaking has been created already or not. If it has jump straight to the account home to prevent
    // the user attempting to complete the create undertaking journey again.
    escService
      .retrieveUndertaking(eori)
      .toContext
      .foldF(renderPage)(_ => Redirect(routes.AccountController.getAccountPage().url).toFuture)

  }

  def postEoriCheck: Action[AnyContent] = enrolled.async { implicit request =>
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
              if (journey.isComplete) Redirect(routes.AccountController.getAccountPage()).toFuture
              else journey.next
            }
      )
  }

  def getIncorrectEori: Action[AnyContent] = enrolled.async { implicit request =>
    Ok(incorrectEoriPage()).toFuture
  }

}
