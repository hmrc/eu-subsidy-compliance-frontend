/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class BaseController(mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with I18nSupport with Logging {

  protected def handleMissingSessionData(dataLabel: String) =
    throw new IllegalStateException(s"$dataLabel data missing on session")

  protected def runStepIfEligible(journey: Journey)(f: => Future[Result])(implicit r: Request[_]): Future[Result] =
    if (journey.isEligibleForStep) f
    else Redirect(journey.previous).toFuture

}
