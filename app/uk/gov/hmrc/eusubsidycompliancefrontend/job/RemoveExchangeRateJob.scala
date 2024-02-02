/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.job
import cats.implicits.toFunctorOps
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveExchangeRateJob @Inject() (
  lifecycle: ApplicationLifecycle,
  mongoComponent: MongoComponent
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends Logging {

  def runExchangeRateRepoJob(): Unit = {
    logger.info("=== clearing ExchangeRate Repository job starting ===")
    mongoComponent.database
      .getCollection("exchangeRateMonthlyCache")
      .drop()
      .toFuture()
      .void
      .map(_ => logger.info(s"=== exchangeRate cache cleared ==="))
  }

  if (appConfig.clearExchangeRateJob) {
    logger.info("=== RemoveEmailJob starting ===")
    runExchangeRateRepoJob()
  } else logger.info("removeEmailJobEnabled flag is false")

  // Shut-down hook
  lifecycle.addStopHook { () =>
    Future.successful(())
  }
  //...
}
