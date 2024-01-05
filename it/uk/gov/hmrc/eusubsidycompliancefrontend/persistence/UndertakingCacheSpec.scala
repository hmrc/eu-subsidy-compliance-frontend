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
import org.mongodb.scala.model.IndexModel
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.transport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingStatus.active
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, IndustrySectorLimit, SubsidyAmount, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.IntegrationBaseSpec
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class UndertakingCacheSpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[CacheItem] {

  override protected val repository = new UndertakingCache(mongoComponent)
  override protected lazy val indexes: Seq[IndexModel] = repository.allIndexes

  private val eori1 = EORI("GB123456789012")
  private val eori2 = EORI("GB123456789013")

  private val undertakingRef = UndertakingRef("UR123456")

  private val businessEntity1 = BusinessEntity(EORI(eori1), leadEORI = true)
  private val businessEntity2 = BusinessEntity(EORI(eori2), leadEORI = false)

  private val undertaking = Undertaking(
    undertakingRef,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34),
    LocalDate.of(2021, 1, 18).some,
    active.some,
    List(businessEntity1, businessEntity2)
  )

  private val undertakingSubsidies = UndertakingSubsidies(
    undertakingIdentifier = undertakingRef,
    nonHMRCSubsidyTotalEUR = SubsidyAmount(0.0),
    nonHMRCSubsidyTotalGBP = SubsidyAmount(0.0),
    hmrcSubsidyTotalEUR = SubsidyAmount(0.0),
    hmrcSubsidyTotalGBP = SubsidyAmount(0.0),
    nonHMRCSubsidyUsage = List.empty,
    hmrcSubsidyUsage = List.empty
  )

  "UndertakingCache" when {

    "get Undertaking is called" must {

      "return None when the cache is empty" in {
        repository.get[Undertaking](eori1).futureValue shouldBe None
      }

      "return None where there is no matching item in the cache" in {
        repository.put(eori1, undertaking).futureValue shouldBe undertaking
        repository.get[Undertaking](eori2).futureValue shouldBe None
      }

      "return the item where present in the cache" in {
        repository.put(eori1, undertaking).futureValue shouldBe undertaking
        repository.get[Undertaking](eori1).futureValue should contain(undertaking)
      }
    }

    "deleteUndertaking is called" must {

      "do nothing if no matching undertaking references are found" in {
        repository.put(eori1, undertaking).futureValue shouldBe undertaking
        repository.deleteUndertaking(UndertakingRef("foo")).futureValue shouldBe (())
        repository.get[Undertaking](eori1).futureValue should contain(undertaking)
      }

      "delete matching undertakings with the specified ref" in {
        repository.put(eori1, undertaking).futureValue shouldBe undertaking
        repository.put(eori2, undertaking).futureValue shouldBe undertaking
        repository.deleteUndertaking(undertakingRef).futureValue shouldBe (())
        repository.get[Undertaking](eori1).futureValue shouldBe None
        repository.get[Undertaking](eori2).futureValue shouldBe None
      }

      "leave undertaking subsidies but not undertakings present in the cache" in {
        repository.put(eori1, undertaking).futureValue shouldBe undertaking
        repository.put(eori2, undertaking).futureValue shouldBe undertaking
        repository.put[UndertakingSubsidies](eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.put[UndertakingSubsidies](eori2, undertakingSubsidies).futureValue shouldBe undertakingSubsidies

        repository.deleteUndertaking(undertakingRef).futureValue shouldBe (())

        repository.get[Undertaking](eori1).futureValue shouldBe None
        repository.get[Undertaking](eori2).futureValue shouldBe None
        repository.get[UndertakingSubsidies](eori1).futureValue should contain(undertakingSubsidies)
        repository.get[UndertakingSubsidies](eori2).futureValue should contain(undertakingSubsidies)
      }

    }

    "get UndertakingSubsidies is called" must {

      "return None when the cache is empty" in {
        repository.get[UndertakingSubsidies](eori1).futureValue shouldBe None
      }

      "return None where there is no matching item in the cache" in {
        repository.put(eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.get[UndertakingSubsidies](eori2).futureValue shouldBe None
      }

      "return the item where present in the cache" in {
        repository.put(eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.get[UndertakingSubsidies](eori1).futureValue should contain(undertakingSubsidies)
      }

    }

    "deleteUndertakingSubsidies is called" must {

      "do nothing if no matching undertaking references are found" in {
        repository.put(eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.deleteUndertakingSubsidies(UndertakingRef("foo")).futureValue shouldBe (())
        repository.get[UndertakingSubsidies](eori1).futureValue should contain(undertakingSubsidies)
      }

      "delete matching undertaking subsidies with the specified ref" in {
        repository.put(eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.put(eori2, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.deleteUndertakingSubsidies(undertakingRef).futureValue shouldBe (())
        repository.get[UndertakingSubsidies](eori1).futureValue shouldBe None
        repository.get[UndertakingSubsidies](eori2).futureValue shouldBe None
      }

      "leave undertakings but not undertaking subsidies present in the cache" in {
        repository.put[Undertaking](eori1, undertaking).futureValue shouldBe undertaking
        repository.put[Undertaking](eori2, undertaking).futureValue shouldBe undertaking
        repository.put[UndertakingSubsidies](eori1, undertakingSubsidies).futureValue shouldBe undertakingSubsidies
        repository.put[UndertakingSubsidies](eori2, undertakingSubsidies).futureValue shouldBe undertakingSubsidies

        repository.deleteUndertakingSubsidies(undertakingRef).futureValue shouldBe (())

        repository.get[Undertaking](eori1).futureValue should contain(undertaking)
        repository.get[Undertaking](eori2).futureValue should contain(undertaking)
        repository.get[UndertakingSubsidies](eori1).futureValue shouldBe None
        repository.get[UndertakingSubsidies](eori2).futureValue shouldBe None
      }

    }

  }

}
