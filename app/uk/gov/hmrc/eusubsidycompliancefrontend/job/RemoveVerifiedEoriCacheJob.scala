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
class RemoveVerifiedEoriCacheJob @Inject() (
  lifecycle: ApplicationLifecycle,
  mongoComponent: MongoComponent
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends Logging {

  //Note: the existing verifiedEoriCache collection needs to be dropped as it has a TTL field of type 'String'.
  //This means the records will never auto delete. The changes in this branch will fix the issue.
  //This job will be ran once and then disabled.
  def runJob(): Unit = {
    logger.info("=== remove verifiedEoriCache job starting ===")
    mongoComponent.database
      .getCollection("verifiedEoriCache")
      .drop()
      .toFuture()
      .void
      .map(_ => logger.info(s"=== verifiedEoriCache dropped ==="))
  }

  if (appConfig.removeVerifiedEoriCacheJob) {
    logger.info("=== run-remove-verifiedEoriCache-job-enabled flag is true ===")
    runJob()
  } else logger.info("run-remove-verifiedEoriCache-job-enabled flag is false")

  // Shut-down hook
  lifecycle.addStopHook { () =>
    Future.successful(())
  }
}
