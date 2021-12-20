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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, PhoneNumber, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessEntityController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector,
  addBusinessPage: AddBusinessPage,
  eoriPage: BusinessEntityEoriPage,
  businessEntityContactPage: BusinessEntityContactPage
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def getAddBusinessEntity: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    for {
      a <- store.get[Undertaking]
      b <- store.get[BusinessEntityJourney]
    } yield (a, b) match {
      case (Some(undertaking), Some(journey)) =>
        journey
          .addBusiness
          .value
          .fold(
              Ok(addBusinessPage(
                addBusinessForm,
                undertaking.name
              ))
          ){x =>
              Ok(addBusinessPage(
                addBusinessForm.fill(FormValues(x.toString)),
                undertaking.name
              ))
          }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[Undertaking].flatMap { undertaking =>
      val name: UndertakingName = undertaking.map(_.name).getOrElse(throw new IllegalStateException("missing undertaking name"))
      addBusinessForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(addBusinessPage(errors, name))),
        form => {
          store.update[BusinessEntityJourney]({ x =>
            x.map { y =>
              y.copy(addBusiness  = y.addBusiness.copy(value = Some(form.value.toBoolean)))
            }
          }).flatMap(_.next)
        }
      )

    }
  }

  def getEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[BusinessEntityJourney](store).flatMap { previous =>
      store.get[BusinessEntityJourney].flatMap {
        case Some(journey) =>
          journey
            .eori
            .value
            .fold(
              Future.successful(
                Ok(eoriPage(
                  eoriForm,
                  previous
                ))
              )
            ) { x =>
              Future.successful(
                Ok(eoriPage(
                  eoriForm.fill(FormValues(x.toString)),
                  previous
                ))
              )
            }
      }

    }
  }

  def postEori: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[BusinessEntityJourney](store).flatMap { previous =>
      eoriForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(eoriPage(errors, previous))),
        form => {
          store.update[BusinessEntityJourney]({ x =>
            x.map { y =>
              y.copy(eori = y.eori.copy(value = Some(EORI(form.value))))
            }
          }).flatMap(_.next)
        }
      )

    }
  }

  def getContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[BusinessEntityJourney].flatMap {
      case Some(journey) =>
        journey
          .contact
          .value
          .fold(
            Future.successful(
              Ok(businessEntityContactPage(
                contactForm,
                journey.previous
              ))
            )
          ){x =>
            Future.successful(
              Ok(businessEntityContactPage(
                contactForm.fill(OneOf(x.phone.map(_.toString), x.mobile.map(_.toString))),
                journey.previous
              ))
            )
          }
    }
  }

  def postContact: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    getPrevious[UndertakingJourney](store).flatMap { previous =>
      contactForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(businessEntityContactPage(errors, previous))),
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




  lazy val addBusinessForm: Form[FormValues] = Form(
    mapping("addBusiness" -> mandatory("addBusiness"))(FormValues.apply)(FormValues.unapply))

  lazy val eoriForm: Form[FormValues] = Form(
    mapping("businessEntityEori" -> mandatory("businessEntityEori"))(FormValues.apply)(FormValues.unapply).verifying(
    "regex.error",
    fields => fields match {
      case a if a.value.matches(EORI.regex) => true
      case _ => false
    }
  ))

}