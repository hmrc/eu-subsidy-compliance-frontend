package uk.gov.hmrc.eusubsidycompliancefrontend.util

import java.time.{LocalDate, Month}

object TaxYearHelpers {

  def taxYearStartForDate(d: LocalDate): LocalDate = {
    val taxYearStartForDateYear = LocalDate.of(d.getYear, Month.APRIL, 6)
    if (d.isBefore(taxYearStartForDateYear)) taxYearStartForDateYear.minusYears(1)
    else taxYearStartForDateYear
  }

}
