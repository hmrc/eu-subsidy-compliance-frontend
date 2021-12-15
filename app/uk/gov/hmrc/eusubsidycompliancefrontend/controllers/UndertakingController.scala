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
import play.api.data.Forms._
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ContactDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, PhoneNumber, Sector, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EligibilityJourney, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector,
  undertakingNamePage: UndertakingNamePage,
  undertakingSectorPage: UndertakingSectorPage,
  undertakingContactPage: UndertakingContactPage,
  cyaPage: UndertakingCheckYourAnswersPage
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

  lazy val undertakingSectorForm: Form[FormValues] = Form(
    mapping("undertakingSector" -> mandatory("undertakingSector"))(FormValues.apply)(FormValues.unapply))

  case class OneOf(a: Option[String], b: Option[String]){
    def toContactDetails =
      ContactDetails(
        a.map(PhoneNumber(_)),
        b.map(PhoneNumber(_))
      )
  }

  val undertakingContactForm: Form[OneOf] = Form(
    mapping(
      "phone" -> optional(text),
      "mobile"  -> optional(text)
    )(OneOf.apply)(OneOf.unapply).verifying(
      "one.or.other.mustbe.present",
      fields => fields match {
        case OneOf(Some(a), Some(b)) if a.matches(PhoneNumber.regex) && b.matches(PhoneNumber.regex)  => true
        case OneOf(_, Some(b)) if b.matches(PhoneNumber.regex) => true
        case OneOf(Some(a),_) if a.matches(PhoneNumber.regex) => true
        case _ => false
      }
    )

  )

  lazy val cyaForm: Form[FormValues] = Form(
    mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))


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
                  undertakingNameForm
                ))
              )
            ){x =>
              Future.successful(
                Ok(undertakingNamePage(
                  undertakingNameForm.fill(FormValues(x))
                ))
              )
            }
        case None => // initialise the empty Journey model
          store.put(UndertakingJourney()).map { _ =>
            Ok(undertakingNamePage(undertakingNameForm))
          }

      }
  }

  def postUndertakingName: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    undertakingNameForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(undertakingNamePage(errors))),
      form => {
        store.update[UndertakingJourney]({ x =>
          x.map { y =>
            y.copy(name = y.name.copy(value = Some(form.value)))
          }
        }).flatMap(_.next)
      }
    )
  }

  def getSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        journey
          .sector
          .value
          .fold(
            Future.successful(
              Ok(undertakingSectorPage(
                undertakingSectorForm,
                journey.previous
              ))
            )
          ){x =>
            Future.successful(
              Ok(undertakingSectorPage(
                undertakingSectorForm.fill(FormValues(x.id.toString)),
                journey.previous
              ))
            )
          }
    }
  }

  def postSector: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      undertakingSectorForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(undertakingSectorPage(errors, previous))),
        form => {
          store.update[UndertakingJourney]({ x =>
            x.map { y =>
              y.copy(sector = y.sector.copy(value = Some(Sector(form.value.toInt))))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        journey
          .contact
          .value
          .fold(
            Future.successful(
              Ok(undertakingContactPage(
                undertakingContactForm,
                journey.previous
              ))
            )
          ){x =>
            Future.successful(
              Ok(undertakingContactPage(
                undertakingContactForm.fill(OneOf(x.phone.map(_.toString), x.mobile.map(_.toString))),
                journey.previous
              ))
            )
          }
    }
  }

  def postContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      undertakingContactForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(undertakingContactPage(errors, previous))),
        form => {
          store.update[UndertakingJourney]({ x =>
            x.map { y =>
              y.copy(contact = y.contact.copy(value = Some(form.toContactDetails)))
            }
          }).flatMap(_.next)
        }
      )
    }
  }

  def getCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) =>
        Future.successful(
          Ok(
            cyaPage(
              journey.name.value.fold(throw new IllegalStateException("name should be defined"))(UndertakingName(_)),
              eori,
              journey.sector.value.getOrElse(throw new IllegalStateException("sector should be defined")),
              journey.contact.value.getOrElse(throw new IllegalStateException("contact should be defined")),
              journey.previous
            )
          )
        )
    }
  }

  def postCheckAnswers: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    cyaForm.bindFromRequest().fold(
      _ => throw new IllegalStateException("value hard-coded, form hacking?"),
      form => {
        store.update[UndertakingJourney]({ x =>
          x.map { y =>
            y.copy(cya = y.cya.copy(value = Some(form.value.toBoolean)))
          }
        }).flatMap { _ =>
          // TODO send undertaking to BE, fwd to next step, copy Sector changes to other services
          Future.successful(Redirect(routes.HelloWorldController.helloWorld()))
        }
      }
    )

  }
}
