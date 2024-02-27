/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{CheckEoriPage, IncorrectEoriPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EligibilityEoriCheckController @Inject() (
  mcc: MessagesControllerComponents,
  checkEoriPage: CheckEoriPage,
  incorrectEoriPage: IncorrectEoriPage,
  actionBuilders: ActionBuilders,
  escService: EscService,
  override val store: Store
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with ControllerFormHelpers {
  import actionBuilders._

  private val eoriCheckForm = formWithSingleMandatoryField("eoricheck")
  private val doYouClaimUrl = routes.EligibilityDoYouClaimController.getDoYouClaim.url

  def getEoriCheck: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    logger.info("EligibilityEoriCheckController.getEoriCheck")
    def renderPage = {
      // At this stage, we have an enrolment, so the user must be eligible to use the service. Thus we can provide
      // defaults for the first two eligibility questions to reflect this.
      val eligibilityJourney = EligibilityJourney()
        .withDoYouClaim(false)
        .withWillYouClaim(true)

      store.getOrCreate[EligibilityJourney](eligibilityJourney).map { journey =>
        val form =
          journey.eoriCheck.value.fold(eoriCheckForm)(eoriCheck => eoriCheckForm.fill(FormValues(eoriCheck.toString)))

        val backLink = request.headers
          .get("Referer")
          .getOrElse(doYouClaimUrl)

        logger.info(s"Showing checkEoriPage for $eori")

        Ok(checkEoriPage(form, eori, backLink))
      }
    }

    // Check if the undertaking has been created already or not. If it has jump straight to the account home to prevent
    // the user attempting to complete the create undertaking journey again.
    escService
      .retrieveUndertaking(eori)
      .toContext
      .foldF(renderPage) { _ =>
        logger.info(s"Redirecting to AccountController.getAccountPage for eori:$eori")
        Redirect(routes.AccountController.getAccountPage.url).toFuture
      }

  }

  def postEoriCheck: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    logger.info("EligibilityEoriCheckController.postEoriCheck")

    eoriCheckForm
      .bindFromRequest()
      .fold(
        errors => {
          logger.info(s"BadRequest to AccountController.postEoriCheck for eori:$eori as a bad request ${errors.errors}")
          val previousJourneyUrl = doYouClaimUrl
          BadRequest(checkEoriPage(errors, eori, previousJourneyUrl)).toFuture
        },
        form =>
          store
            .update[EligibilityJourney](_.setEoriCheck(form.value.toBoolean))
            .flatMap { _ =>
              if (form.value.toBoolean) Redirect(routes.UndertakingController.getAboutUndertaking.url).toFuture
              else Redirect(routes.EligibilityEoriCheckController.getIncorrectEori.url).toFuture
            }
      )
  }

  def getIncorrectEori: Action[AnyContent] = enrolled.async { implicit request =>
    logger.info(s"EligibilityEoriCheckController.getIncorrectEori showing incorrectEoriPage")
    Ok(incorrectEoriPage()).toFuture
  }

}
