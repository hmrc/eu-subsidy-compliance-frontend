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

package uk.gov.hmrc.eusubsidycompliancefrontend.persistence

import uk.gov.hmrc.eusubsidycompliancefrontend.models.VerifiedEori
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1
import uk.gov.hmrc.eusubsidycompliancefrontend.util.IntegrationBaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class VerifiedEoriCacheISpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[VerifiedEori] {

  override protected val repository = new VerifiedEoriCache(mongoComponent)

  private val eori = EORI("GB922456789077")

  private val verifiedEori1 = VerifiedEori(eori)

  "VerifiedEoriCache" should {

    "return None when eori is not found" in {
      repository.get(eori1).futureValue shouldBe None
    }

    "return the VerifiedEori when eori is found" in {
      repository.put(verifiedEori1).futureValue shouldBe ()
      Thread.sleep(10000)
      repository.get(eori).futureValue shouldBe Some(verifiedEori1)
    }

  }

}
