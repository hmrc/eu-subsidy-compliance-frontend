package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class ReleaseCEnabledControllerSpec
  extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EscServiceSupport {

  override def overrideBindings: List[GuiceableModule] = List(
      inSequence {
        bind[AuthConnector].toInstance(mockAuthConnector)
        bind[EscService].toInstance(mockEscService)
        bind[Store].toInstance(mockJourneyStore)
      }
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |features.release-c-enabled  = "true"
                                   |""".stripMargin)
    )
  )

  private val controller = instanceOf[SubsidyController]

  "ClaimConfirmationPage with ReleaseCEnabled being true" when {

    "handling request to get Payment reported Page" must {
      def performAction() = controller.getClaimConfirmationPage(
        FakeRequest(GET, routes.SubsidyController.startFirstTimeUserJourney.url)
      )

      "display the page" when {

        "ClaimConfirmationPage paragraph body is available" in {
          inSequence {
            mockAuthWithEnrolment()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val subsidySubHeading = document.getElementById("claimSubheadingId").text()
          subsidySubHeading shouldBe "What happens next"

          val paraOne = document.getElementById("claimSubsidyNewParagraph").text()
          paraOne shouldBe "It may take up to 24 hours before you can continue to claim any further Customs Duty waivers that you may be entitled to."

          val paraTwo = document.getElementById("claimNewParaId").text()
          paraTwo shouldBe "Your next report must be made by 5 February 2024. This date is 90 days after the missed deadline."
        }
      }
    }
  }

  "NoClaimConfirmationPage with ReleaseCEnabled being true" when {

    "handling request to get Report sent Page" must {
      def performAction() = controller.getReportedNoCustomSubsidyPage(
        FakeRequest(GET, routes.SubsidyController.startFirstTimeUserJourney.url)
      )

      "display the page" when {

        "NoClaimConfirmationPage paragraph body is available" in {
          inSequence {
            mockAuthWithEnrolment()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[SubsidyJourney](eori1)(Right(subsidyJourney))
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val subsidySubHeading = document.getElementById("noClaimSubheadingId").text()
          subsidySubHeading shouldBe "What happens next"

          val paraOne = document.getElementById("noClaimSubsidyNewParagraph").text()
          paraOne shouldBe "It may take up to 24 hours before you can continue to claim any further Customs Duty waivers that you may be entitled to."

          val paraTwo = document.getElementById("noClaimNewParaId").text()
          paraTwo shouldBe "Your next report must be made by 5 February 2024. This date is 90 days after the missed deadline."
        }
      }
    }
  }
}

