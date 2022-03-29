package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.{LocalDate, LocalDateTime}

trait TimeProviderSupport { this: ControllerSpec =>

  val mockTimeProvider = mock[TimeProvider]

  def mockTimeProviderNow(now: LocalDateTime) =
    (mockTimeProvider.now _).expects().returning(now)

  def mockTimeToday(now: LocalDate) =
    (mockTimeProvider.today _).expects().returning(now)

}
