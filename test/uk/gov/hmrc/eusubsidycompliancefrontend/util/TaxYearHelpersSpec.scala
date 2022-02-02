package uk.gov.hmrc.eusubsidycompliancefrontend.util

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.LocalDate

class TaxYearHelpersSpec extends AnyWordSpecLike with Matchers {

  "taxYearStartForDate" must {

    "return a tax year starting in the same year if the date falls on the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-04-06")) mustBe LocalDate.parse("2022-04-06")
    }

    "return a tax year starting in the same year if the date falls after the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-12-06")) mustBe LocalDate.parse("2022-04-06")
    }

    "return a tax year starting in the previous year if the date falls before the 6th of April" in {
      TaxYearHelpers.taxYearStartForDate(LocalDate.parse("2022-02-06")) mustBe LocalDate.parse("2021-04-06")
    }

  }

}
