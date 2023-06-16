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

import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.eusubsidycompliancefrontend.models.NonHmrcSubsidy
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, SubsidyRef, TraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.IntegrationBaseSpec
import uk.gov.hmrc.mongo.cache.{CacheItem, MongoCacheRepository}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class RemovedSubsidyRepositorySpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[CacheItem] {

  private val underTest = new RemovedSubsidyRepository(mongoComponent)

  override protected def repository: MongoCacheRepository[EORI] = underTest.repository

  private val eori1 = EORI("GB123456789012")
  private val eori2 = EORI("GB123456789013")

  private val nonHmrcSubsidy = NonHmrcSubsidy(
    subsidyUsageTransactionId = Some(SubsidyRef("AB12345")),
    allocationDate = LocalDate.of(2022, 1, 1),
    submissionDate = LocalDate.of(2022, 2, 2),
    publicAuthority = "Local Authority".some,
    traderReference = TraderRef("ABC123").some,
    nonHMRCSubsidyAmtEUR = SubsidyAmount(BigDecimal(543.21)),
    businessEntityIdentifier = eori1.some,
    amendmentType = EisSubsidyAmendmentType("1").some
  )

  "RemovedSubsidyRepository" should {

    "return an empty list for a given EORI when the repository is empty" in {
      underTest.getAll(eori1).futureValue shouldBe List.empty
    }

    "return an empty list for a given EORI when the repository contains no data for that EORI" in {
      underTest.add(eori1, nonHmrcSubsidy).futureValue shouldBe (())

      underTest.getAll(eori2).futureValue shouldBe List.empty
    }

    "return a list of payments for a given EORI when the repository contains data for that EORI" in {
      underTest.add(eori2, nonHmrcSubsidy).futureValue shouldBe (())
      underTest.add(eori2, nonHmrcSubsidy).futureValue shouldBe (())

      underTest.getAll(eori2).futureValue shouldBe List(nonHmrcSubsidy, nonHmrcSubsidy)
    }
  }

}
