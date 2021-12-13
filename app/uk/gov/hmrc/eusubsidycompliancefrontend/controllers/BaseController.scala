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
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class BaseController @Inject()(mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport {

  protected def mandatory(key: String): Mapping[String] = {
    text.transform[String](_.trim, s => s).verifying(required(key))
  }

  protected def mandatoryAndValid(key: String, regex: String): Mapping[String] = {
    text.transform[String](_.trim, s => s).verifying(combine(required(key), constraint(key, regex)))
  }

  private def combine[T](c1: Constraint[T], c2: Constraint[T]): Constraint[T] = Constraint { v =>
    c1.apply(v) match {
      case Valid => c2.apply(v)
      case i: Invalid => i
    }
  }

  private def required(key: String): Constraint[String] = Constraint {
    case "" => Invalid(s"error.$key.required")
    case _ => Valid
  }

  private def constraint(key: String, regex: String): Constraint[String] = Constraint {
    case a if !a.matches(regex) => Invalid(s"error.$key.invalid")
    case _ => Valid
  }
}