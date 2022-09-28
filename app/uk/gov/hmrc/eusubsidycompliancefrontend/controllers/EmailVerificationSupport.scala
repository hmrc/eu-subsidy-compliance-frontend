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

import cats.implicits.catsSyntaxOptionId
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, OptionalEmailFormInput}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EmailVerificationService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationSupport extends FormHelpers { this: FrontendController =>

  protected val emailService: EmailService
  protected val emailVerificationService: EmailVerificationService

  protected implicit val executionContext: ExecutionContext

  protected val optionalEmailForm: Form[OptionalEmailFormInput] = Form(
    mapping(
      "using-stored-email" -> mandatory("using-stored-email"),
      "email" -> mandatoryIfEqual("using-stored-email", "false", email)
    )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)
  )

  protected val emailForm: Form[FormValues] = Form(
    mapping(
      "email" -> email
    )(FormValues.apply)(FormValues.unapply)
  )

  protected def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] = {
    emailVerificationService
      .getEmailVerification(eori)
      .toContext
      .foldF(retrieveCdsEmail)(_.email.some.toFuture)
  }

  private def retrieveCdsEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] =
    emailService
      .retrieveEmailByEORI(eori)
      .map {
        case RetrieveEmailResponse(EmailType.VerifiedEmail, email) => email.map(_.value)
        case _ => Option.empty
      }

}
