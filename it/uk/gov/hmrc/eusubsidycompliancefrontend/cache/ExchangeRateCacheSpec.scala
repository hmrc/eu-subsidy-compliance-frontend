package uk.gov.hmrc.eusubsidycompliancefrontend.cache

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ExchangeRate
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class ExchangeRateCacheSpec
  extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[CacheItem]
    with ScalaFutures
    with DefaultAwaitTimeout
    with Matchers {

  override protected def repository = new ExchangeRateCache(mongoComponent)

  private val yearMonth1 = YearAndMonth(2022, 1)
  private val yearMonth2 = YearAndMonth(2022, 6)

  private val exchangeRate1 = ExchangeRate("EUR", "GBP", BigDecimal(0.867))
  private val exchangeRate2 = ExchangeRate("EUR", "GBP", BigDecimal(0.807))

  "ExchangeRateCache" should {

    "return None when the cache is empty" in {
      repository.get[YearAndMonth](yearMonth1).futureValue shouldBe None
    }

    "return None when there is no matching item in the cache" in {
      repository.put(yearMonth1, exchangeRate1).futureValue shouldBe exchangeRate1
      repository.get[YearAndMonth](yearMonth2).futureValue shouldBe None
    }

    "return the item when present in the cache" in {
      repository.put(yearMonth2, exchangeRate2).futureValue shouldBe exchangeRate2
      Thread.sleep(10000)
      repository.get[YearAndMonth](yearMonth2).futureValue shouldBe exchangeRate2
    }

  }

}
