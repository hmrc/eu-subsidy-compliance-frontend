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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.{Json, OFormat}
import play.api.test.DefaultAwaitTimeout
import shapeless.tag.@@
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.JourneyStore
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyStoreSpec
  extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[CacheItem]
    with ScalaFutures
    with DefaultAwaitTimeout
    with Matchers {

  private case class Thing(foo: String)
  private case class AnotherThing(bar: Int)

  private val thing = Thing("foo")
  private val anotherThing = AnotherThing(42)

  private implicit val thingFormat: OFormat[Thing] = Json.format[Thing]
  private implicit val anotherThingFormat: OFormat[AnotherThing] = Json.format[AnotherThing]

  private implicit val eori: String @@ types.EORI.Tag = EORI("GB123456789012")
  private val anotherEori = EORI("GB098765432109")

  override protected def repository = new JourneyStore(mongoComponent, Configuration.empty)

  "JourneyStore" when {

    "put is called" must {

      "return success where the cache is empty" in {
        repository.put[Thing](thing).futureValue shouldBe thing
      }

      "return success when replacing an item in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing
        repository.put[Thing](thing).futureValue shouldBe thing
      }

    }

    "get is called" must {

      "return None when the cache is empty" in {
        repository.get[Thing].futureValue shouldBe None
      }

      "return None where there is no matching item in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing
        repository.put[AnotherThing](anotherThing)(anotherEori, anotherThingFormat).futureValue shouldBe anotherThing

        repository.get[AnotherThing].futureValue shouldBe None
      }

      "return the item where present in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing

        repository.get[Thing].futureValue should contain(thing)
      }

    }


    "getOrCreate is called" must {

      "return the cached item where it is present in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing
        repository.getOrCreate(Thing("bar")).futureValue shouldBe thing
      }

      "store and return the supplied value in the cache where there is no cached value" in {
        repository.getOrCreate(thing).futureValue shouldBe thing
      }

    }

    "delete is called" must {

      "return success if the cache is empty" in {
        repository.delete[Thing].futureValue shouldBe (())
      }

      "delete the specified object but leave other objects in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing
        repository.put[AnotherThing](anotherThing).futureValue shouldBe anotherThing

        repository.delete[Thing].futureValue shouldBe (())

        repository.get[Thing].futureValue shouldBe None
        repository.get[AnotherThing].futureValue should contain(anotherThing)
      }

    }

    "update is called" must {

      "update an existing item in the cache" in {
        repository.put[Thing](thing).futureValue shouldBe thing

        val updatedThing = Thing("bar")

        repository.update[Thing](_ => updatedThing).futureValue shouldBe updatedThing
        repository.get[Thing].futureValue should contain(updatedThing)
      }

      "fail if there is no item to update in the cache" in {
        repository.get[Thing].futureValue shouldBe None
        repository.update[Thing](_ => thing).failed.futureValue shouldBe an[IllegalStateException]
      }

    }

    "deleteAll is called" must {

      "return success if there are no cached items for the specified EORI" in {
        repository.deleteAll.futureValue shouldBe (())
      }

      "remove all data for the specified EORI" in {
        repository.put[Thing](thing).futureValue shouldBe thing
        repository.put[AnotherThing](anotherThing).futureValue shouldBe anotherThing

        repository.deleteAll.futureValue shouldBe (())

        repository.get[Thing].futureValue shouldBe None
        repository.get[AnotherThing].futureValue shouldBe None
      }
    }

  }

}
