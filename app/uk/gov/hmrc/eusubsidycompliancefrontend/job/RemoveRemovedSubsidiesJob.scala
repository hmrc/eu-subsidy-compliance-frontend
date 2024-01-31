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
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveRemovedSubsidiesJob @Inject() (
  lifecycle: ApplicationLifecycle,
  mongoComponent: MongoComponent
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends Logging {

  def runJob(): Unit = {
    logger.warn("=== remove removedSubsidies job starting ===")
    mongoComponent.database
      .getCollection("removedSubsidies")
      .drop()
      .toFuture()
      .void
      .map(_ => logger.warn(s"=== removedSubsidies cache cleared ==="))
  }

  if (appConfig.removeRemovedSubsidiesJob) {
    logger.warn("=== run-remove-removed-subsidies-job-enabled flag is true ===")
    runJob()
  } else logger.warn("run-remove-removed-subsidies-job-enabled flag is false")

  // Shut-down hook
  lifecycle.addStopHook { () =>
    Future.successful(())
  }
  //...
}
