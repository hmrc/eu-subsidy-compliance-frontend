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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscVerifiedEmailActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ExchangeRateTestController @Inject() (
                                             mcc: MessagesControllerComponents,
                                             escCDSActionBuilders: EscVerifiedEmailActionBuilders,
                                             escService: EscService
)(implicit val ec: ExecutionContext) extends BaseController(mcc) {

  import escCDSActionBuilders._

  def getExchangeRate(isoDate: String): Action[AnyContent] = withVerifiedEmailAuthenticatedUser.async { implicit request =>
    val parsedDate = LocalDate.parse(isoDate)

    escService.retrieveExchangeRate(parsedDate)
      .map(r => Ok(Json.toJson(r)))
  }

}
