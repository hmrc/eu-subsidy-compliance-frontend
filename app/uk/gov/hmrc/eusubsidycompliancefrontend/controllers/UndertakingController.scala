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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EligibilityJourney, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector,
  checkEoriPage: CheckEoriPage,
  incorrectEoriPage: IncorrectEoriPage,
  createUndertakingPage: CreateUndertakingPage,
  undertakingNamePage: UndertakingNamePage,
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  lazy val eoriCheckForm : Form[FormValues] = Form(
    mapping("eoricheck" -> mandatory("eoricheck"))(FormValues.apply)(FormValues.unapply))

  lazy val createUndertakingForm: Form[FormValues] = Form(
    mapping("createUndertaking" -> mandatory("createUndertaking"))(FormValues.apply)(FormValues.unapply))

  lazy val undertakingNameForm: Form[FormValues] = Form(
    mapping("undertakingName" -> mandatory("undertakingName"))(FormValues.apply)(FormValues.unapply))

  def getEoriCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        journey
          .eoriCheck
          .value
          .fold(
            Future.successful(
              Ok(checkEoriPage(
                eoriCheckForm,
                eori
              ))
            )
          ){x =>
            Future.successful(
              Ok(checkEoriPage(
                eoriCheckForm.fill(FormValues(x.toString)),
                eori
              ))
            )
          }
      case None =>
        store.put(UndertakingJourney()).map { _ =>
          Ok(checkEoriPage(eoriCheckForm, eori))
        }
    }
  }

  def postEoriCheck: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    eoriCheckForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(checkEoriPage(errors, eori))),
      form => {
        store.update[UndertakingJourney]({ x =>
          x.map { y =>
            y.copy(eoriCheck = y.eoriCheck.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap(_.next)
      }
    )
  }

  def getIncorrectEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    Future.successful(Ok(incorrectEoriPage()))
  }

  def getCreateUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
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
              y.copy(acceptTerms = y.acceptTerms.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap { _ =>
            Future.successful(Redirect(routes.UndertakingController.getUndertakingName()))
          }
        }
      )
  }

  def getUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
      store.get[UndertakingJourney].flatMap {
        case Some(journey) =>
          journey
            .name
            .value
            .fold(
              Future.successful(
                Ok(undertakingNamePage(
                  undertakingNameForm,
                  journey.previous
                ))
              )
            ){x =>
              Future.successful(
                Ok(undertakingNamePage(
                  undertakingNameForm.fill(FormValues(x)),
                  journey.previous
                ))
              )
            }
      }
  }

  def postUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      undertakingNameForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(undertakingNamePage(errors, previous))),
        form => {
          store.update[UndertakingJourney]({ x =>
            x.map { y =>
              y.copy(name = y.name.copy(value = Some(form.value)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }
}
