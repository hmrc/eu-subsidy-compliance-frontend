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

package uk.gov.hmrc.eusubsidycompliancefrontend.job

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

import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{EoriEmailRepository, JourneyStore}

import javax.inject._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveEmailJob @Inject() (
  lifecycle: ApplicationLifecycle,
  eoriEmailRepository: EoriEmailRepository,
  journeyStore: JourneyStore
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends Logging {

  def runEoriEmailRepositoryJob(): Unit = {
    logger.warn("=== remove email from eoriEmailRepository starting ===")
    eoriEmailRepository.removeEmailField().map(_ => jobCompleted("eoriEmailRepository"))
  }

  def runJourneyStoryJob(): Unit = {
    logger.warn("=== remove email from journeyStore starting ===")
    journeyStore.removeVerifiedEmailField().map(_ => jobCompleted("journeyStore"))
  }
  def jobCompleted(repoName: String): Unit = logger.warn(s"=== remove  email address from $repoName repo completed ===")

  if (appConfig.removeEmailJobEnabled) {
    logger.warn("=== RemoveEmailJob starting ===")
    runEoriEmailRepositoryJob()
    runJourneyStoryJob()
  } else logger.warn("removeEmailJobEnabled flag is false")

  // Shut-down hook
  lifecycle.addStopHook { () =>
    Future.successful(())
  }
  //...
}
