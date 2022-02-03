package uk.gov.hmrc.eusubsidycompliancefrontend.testutil

import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.{LocalDate, ZoneId}

object FakeTimeProvider {

  def withFixedDate(day: Int, month: Int, year: Int):TimeProvider = new TimeProvider {
    override def today: LocalDate = LocalDate.of(year, month, day)
    override def today(z: ZoneId): LocalDate = today
  }

}
