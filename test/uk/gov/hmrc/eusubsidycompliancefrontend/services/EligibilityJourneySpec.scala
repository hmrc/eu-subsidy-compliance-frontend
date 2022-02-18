package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class EligibilityJourneySpec extends AnyWordSpecLike with Matchers {

  "EligibilityJourney" when {

    "steps is called" should {

      "return all forms at the start of the journey" in {
        val underTest = EligibilityJourney()
        underTest.steps.flatten shouldBe List(
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.notEligible,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.createUndertaking
        )
      }
    }

  }

}
