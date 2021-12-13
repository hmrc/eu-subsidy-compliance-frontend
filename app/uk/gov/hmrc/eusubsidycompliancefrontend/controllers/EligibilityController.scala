/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EligibilityJourney, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibilityController @Inject()(
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage,
  customsWaiversPage: CustomsWaiversPage,
  willYouClaimPage: WillYouClaimPage,
  notEligiblePage: NotEligiblePage,
  mainBusinessCheckPage: MainBusinessCheckPage,
  notEligibleToLeadPage: NotEligibleToLeadPage,
  termsPage: EligibilityTermsAndConditionsPage,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  lazy val customsWaiversForm: Form[FormValues] = Form(
    mapping("customswaivers" -> mandatory("customswaivers"))(FormValues.apply)(FormValues.unapply))

  lazy val mainBusinessCheckForm: Form[FormValues] = Form(
    mapping("mainbusinesscheck" -> mandatory("mainbusinesscheck"))(FormValues.apply)(FormValues.unapply))

  lazy val willYouClaimForm: Form[FormValues] = Form(
    mapping("willyouclaim" -> mandatory("willyouclaim"))(FormValues.apply)(FormValues.unapply))

  lazy val termsForm: Form[FormValues] = Form(
    mapping("terms" -> mandatory("terms"))(FormValues.apply)(FormValues.unapply))

  case class FormValues(value: String)

  def getCustomsWaivers: Action[AnyContent] = escAuthentication.async { implicit request =>

    implicit val eori: EORI = request.eoriNumber

    // TODO auth will at some point handle some of this, i.e. we won't come here if there is
    // an undertaking, or a partially filled undertaking
    // If the user has completed this journey but not started an undertaking they will have to redo
    // this journey 30 days later
    for {
      a <- store.get[Undertaking]
      b <- if (a.isEmpty) connector.retrieveUndertaking(eori) else Future.successful(Option.empty)
      c <- store.get[EligibilityJourney]
    } yield (a, b, c) match {
      case (Some(_), _, _) =>
        // TODO redirect to account page
        Ok("account page")
      case (None, Some(undertaking), _) =>
        // TODO initialise UndertakingJourneyModel and store that and the undertaking
        Ok("account page")
      case (_, _, Some(eligibilityJourney)) =>
        val form: Form[FormValues] =
          eligibilityJourney.customsWaivers.value.fold(customsWaiversForm) { x =>
            customsWaiversForm.fill(FormValues(x.toString))
          }
        Ok(customsWaiversPage(form))
      case _ =>
        store.put(EligibilityJourney()) // initialise an empty journey
        Ok(customsWaiversPage(customsWaiversForm))
    }

  }

  def postCustomsWaivers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    customsWaiversForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(customsWaiversPage(errors))),
      form => {
        store.update[EligibilityJourney]({ x =>
          x.map { y =>
            y.copy(customsWaivers = y.customsWaivers.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap(_.next)
      }
    )
  }

  def getWillYouClaim: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[EligibilityJourney].flatMap {
      case Some(journey) =>
        journey
          .willYouClaim
          .value
          .fold(
            Future.successful(
              Ok(willYouClaimPage(
                willYouClaimForm,
                journey.previous
              ))
            )
          ){x =>
            Future.successful(
              Ok(willYouClaimPage(
                willYouClaimForm.fill(FormValues(x.toString)),
                journey.previous
              ))
            )
          }
    }
  }

  def postWillYouClaim: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious.flatMap { previous =>
      willYouClaimForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(willYouClaimPage(errors, previous))),
        form => {
          store.update[EligibilityJourney]({ x =>
            x.map { y =>
              y.copy(willYouClaim = y.willYouClaim.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getNotEligible: Action[AnyContent] = escAuthentication.async { implicit request =>
    Future.successful(Ok(notEligiblePage()))
  }


  def getMainBusinessCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[EligibilityJourney].flatMap {
      case Some(journey) =>
        journey
          .mainBusinessCheck
          .value
          .fold(
            Future.successful(
              Ok(mainBusinessCheckPage(
                mainBusinessCheckForm,
                journey.previous
              ))
            )
          ){x =>
            Future.successful(
              Ok(mainBusinessCheckPage(
                mainBusinessCheckForm.fill(FormValues(x.toString)),
                journey.previous
              ))
            )
          }
      case None => ??? // TODO throw some kind of exception or redirect to previous, shouldn't happen really...
    }
  }

  def postMainBusinessCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious.flatMap { previous =>
      mainBusinessCheckForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(mainBusinessCheckPage(errors, previous))),
        form => {
          store.update[EligibilityJourney]({ x =>
            x.map { y =>
              y.copy(mainBusinessCheck = y.mainBusinessCheck.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getNotEligibleToLead: Action[AnyContent] = escAuthentication.async { implicit request =>
    Future.successful(Ok(notEligibleToLeadPage()))
  }

  def getTerms: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious.flatMap { previous =>
      Future.successful(Ok(termsPage(previous)))
    }
  }

  def postTerms: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious.flatMap { previous =>
      termsForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(mainBusinessCheckPage(errors, previous))),
        form => {
          store.update[EligibilityJourney]({ x =>
            x.map { y =>
              y.copy(acceptTerms = y.acceptTerms.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap { _ =>
            // TODO - this should link to the beginning of the undertaking journey
            Future.successful(Redirect(routes.HelloWorldController.helloWorld()))
          }
        }
      )
    }
  }


  def getPrevious(implicit eori: EORI, request: Request[_]): Future[Uri] =
    store.get[EligibilityJourney].map { x =>
      x.fold(throw new IllegalStateException("journey should be there")){ y =>
        y.previous
      }
    }

}