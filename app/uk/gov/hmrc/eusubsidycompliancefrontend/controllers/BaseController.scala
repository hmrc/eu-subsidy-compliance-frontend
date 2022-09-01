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

import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Mapping}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Singleton

@Singleton
class BaseController(mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport {

  protected def mandatory(key: String): Mapping[String] =
    text.transform[String](_.trim, s => s).verifying(required(key))

  private def required(key: String): Constraint[String] = Constraint {
    case "" => Invalid(s"error.$key.required")
    case _ => Valid
  }

  protected def formWithSingleMandatoryField(fieldName: String): Form[FormValues] = Form(
    mapping(fieldName -> mandatory(fieldName))(FormValues.apply)(FormValues.unapply)
  )

  // TODO - review this
  protected def handleMissingSessionData(dataLabel: String) =
    throw new IllegalStateException(s"$dataLabel data missing on session")

}
