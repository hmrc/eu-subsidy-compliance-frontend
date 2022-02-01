package uk.gov.hmrc.eusubsidycompliancefrontend.models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DateFormValuesSpec extends AnyWordSpecLike with Matchers {

  "DateFormValues" when {

    "isValidDate is called" must {

      "return true for valid date values" in {
        DateFormValues("1", "2", "2022").isValidDate mustBe true
      }

      "return false for invalid date values" in {
        DateFormValues("99", "99", "10").isValidDate mustBe false
      }

    }

  }

}
