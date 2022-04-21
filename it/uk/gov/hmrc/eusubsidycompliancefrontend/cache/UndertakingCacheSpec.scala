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

package uk.gov.hmrc.eusubsidycompliancefrontend.cache

import cats.implicits.catsSyntaxOptionId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.transport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class UndertakingCacheSpec
  extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[CacheItem]
    with ScalaFutures
    with DefaultAwaitTimeout
    with Matchers {

  override protected def repository = new UndertakingCache(mongoComponent)

  private val eori1 = EORI("GB123456789012")
  private val eori2 = EORI("GB123456789013")

  private val undertakingRef = UndertakingRef("UR123456")

  private val businessEntity1 = BusinessEntity(EORI(eori1), leadEORI = true)
  private val businessEntity2 = BusinessEntity(EORI(eori2), leadEORI = false)

  private val undertaking1 = Undertaking(
    undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021, 1, 18).some,
    List(businessEntity1, businessEntity2)
  )

  "UndertakingCache" when {

    "get is called" must {

      "return None when the cache is empty" in {
        repository.get[Undertaking](eori1).futureValue shouldBe None
      }

      "return None where there is no matching item in the cache" in {
        repository.put(eori1, undertaking1).futureValue shouldBe undertaking1
        repository.get[Undertaking](eori2).futureValue shouldBe None
      }

      "return the item where present in the cache" in {
        repository.put(eori1, undertaking1).futureValue shouldBe undertaking1
        repository.get[Undertaking](eori1).futureValue should contain(undertaking1)
      }
    }

    "deleteUndertaking is called" must {

      "do nothing if no matching undertaking references are found" in {
        repository.put(eori1, undertaking1).futureValue shouldBe undertaking1
        repository.deleteUndertaking(UndertakingRef("foo")).futureValue shouldBe (())
        repository.get[Undertaking](eori1).futureValue should contain(undertaking1)
      }

      "delete matching undertakings with the specified ref" in {
        repository.put(eori1, undertaking1).futureValue shouldBe undertaking1
        repository.put(eori2, undertaking1).futureValue shouldBe undertaking1
        repository.deleteUndertaking(undertakingRef).futureValue shouldBe (())
        repository.get[Undertaking](eori1).futureValue shouldBe None
        repository.get[Undertaking](eori2).futureValue shouldBe None
      }

    }

  }

}
