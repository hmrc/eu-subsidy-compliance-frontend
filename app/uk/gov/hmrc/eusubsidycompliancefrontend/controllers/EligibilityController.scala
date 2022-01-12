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
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI}
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
  checkEoriPage: CheckEoriPage,
  incorrectEoriPage: IncorrectEoriPage,
  createUndertakingPage: CreateUndertakingPage,
  accountPage: AccountPage,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def firstEmptyPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[EligibilityJourney].map {
      case Some(journey) =>
        journey
          .firstEmpty
          .fold(
            Redirect(routes.UndertakingController.getUndertakingName())
          )(identity)
    }
  }

  def getCustomsWaivers: Action[AnyContent] = escAuthentication.async { implicit request =>

    implicit val eori: EORI = request.eoriNumber

    store.get[EligibilityJourney].flatMap {
      case Some(journey) =>
        journey
          .customsWaivers
          .value
          .fold(
            Future.successful(
              Ok(customsWaiversPage(
                customsWaiversForm
              ))
            )
          ){x =>
            Future.successful(
              Ok(customsWaiversPage(
                customsWaiversForm.fill(FormValues(x.toString))
              ))
            )
          }
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
    getPrevious[EligibilityJourney](store).flatMap { previous =>
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
    }
  }

  def postMainBusinessCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[EligibilityJourney](store).flatMap { previous =>
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
    getPrevious[EligibilityJourney](store).flatMap { previous =>
      Future.successful(Ok(termsPage(previous)))
    }
  }

  def postTerms: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[EligibilityJourney](store).flatMap { previous =>
      termsForm.bindFromRequest().fold(
        _ => throw new IllegalStateException("value hard-coded, form hacking?"),
        form => {
          store.update[EligibilityJourney]({ x =>
            x.map { y =>
              y.copy(acceptTerms = y.acceptTerms.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getEoriCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[EligibilityJourney](store).flatMap { previous =>
      store.get[EligibilityJourney].flatMap {
        case Some(journey) =>
          journey
            .eoriCheck
            .value
            .fold(
              Future.successful(
                Ok(checkEoriPage(
                  eoriCheckForm,
                  eori,
                  previous
                ))
              )
            ) { x =>
              Future.successful(
                Ok(checkEoriPage(
                  eoriCheckForm.fill(FormValues(x.toString)),
                  eori,
                  previous
                ))
              )
            }
      }
    }
  }

  def postEoriCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[EligibilityJourney](store).flatMap { previous =>
      eoriCheckForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(checkEoriPage(errors, eori, previous))),
        form => {
          store.update[EligibilityJourney]({ x =>
            x.map { y =>
              y.copy(eoriCheck = y.eoriCheck.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getIncorrectEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    Future.successful(Ok(incorrectEoriPage()))
  }

  def getCreateUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[EligibilityJourney](store).flatMap { previous =>
      Future.successful(Ok(createUndertakingPage(previous)))
    }
  }

  def postCreateUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    createUndertakingForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[EligibilityJourney]({ x =>
          x.map { y =>
            y.copy(createUndertaking = y.createUndertaking.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap { _ =>
          Future.successful(Redirect(routes.UndertakingController.getUndertakingName()))
        }
      }
    )
  }

  lazy val customsWaiversForm: Form[FormValues] = Form(
    mapping("customswaivers" -> mandatory("customswaivers"))(FormValues.apply)(FormValues.unapply))

  lazy val mainBusinessCheckForm: Form[FormValues] = Form(
    mapping("mainbusinesscheck" -> mandatory("mainbusinesscheck"))(FormValues.apply)(FormValues.unapply))

  lazy val willYouClaimForm: Form[FormValues] = Form(
    mapping("willyouclaim" -> mandatory("willyouclaim"))(FormValues.apply)(FormValues.unapply))

  lazy val termsForm: Form[FormValues] = Form(
    mapping("terms" -> mandatory("terms"))(FormValues.apply)(FormValues.unapply))

  lazy val eoriCheckForm : Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply))

  lazy val createUndertakingForm: Form[FormValues] = Form(
    mapping("createUndertaking" -> mandatory("createUndertaking"))(FormValues.apply)(FormValues.unapply))

}