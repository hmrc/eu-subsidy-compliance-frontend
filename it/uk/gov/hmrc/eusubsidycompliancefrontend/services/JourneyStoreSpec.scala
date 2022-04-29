package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

// TODO - review all tests to confirm that the correct name is used in the opening statement of the test dsl
class JourneyStoreSpec
  extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[CacheItem]
    with ScalaFutures
    with DefaultAwaitTimeout
    with Matchers
    with MockFactory {

  private case class Thing(foo: String)
  private case class AnotherThing(bar: Int)

  private val thing = Thing("foo")
  private val anotherThing = AnotherThing(42)

  private implicit val thingFormat = Json.format[Thing]
  private implicit val anotherThingFormat = Json.format[AnotherThing]

  private implicit val eori = EORI("GB123456789012")
  private val anotherEori = EORI("GB098765432109")

  override protected def repository = new JourneyStore(mongoComponent, Configuration.empty)

  "JourneyStore" when {

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

    "put is called" must {

      "return success where the cache is empty" in {

      }

      "return success when replacing an item in the cache" in {

      }

    }

    "getOrCreate is called" must {

      "return the cached item where it is present in the cache" in {

      }

      "store and return the supplied value in the cache where there is no cached value" in {

      }

    }

    "delete is called" must {

    }

    "update is called" must {

    }

  }

}
