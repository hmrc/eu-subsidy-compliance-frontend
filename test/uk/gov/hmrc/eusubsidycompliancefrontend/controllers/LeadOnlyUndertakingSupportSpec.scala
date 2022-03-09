package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.implicits.catsSyntaxOptionId
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, MessagesControllerComponents}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.Fixtures.eori
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.CommonTestData.{eori3, undertaking}

import scala.concurrent.{ExecutionContext, Future}

class LeadOnlyUndertakingSupportSpec extends AnyWordSpecLike with MockFactory with ScalaFutures with Matchers {

  private val mockEscService = mock[EscService]

  private val underTest = new FrontendController(mock[MessagesControllerComponents]) with LeadOnlyUndertakingSupport {
    override protected val escService: EscService = mockEscService
    override protected implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  }

  "LeadOnlyUnderTakingSupport" should {

    "invoke the function" when {

      "called with a request from a lead undertaking user" in {
        mockRetrieveUndertaking(eori)(undertaking.some.toFuture)

        val fakeRequest = authorisedRequestForEori(eori)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest)

        status(result) shouldBe OK
      }
    }

    "redirect to the account home page" when {

      "called with a request from a non-lead undertaking user" in {
        mockRetrieveUndertaking(eori3)(undertaking.some.toFuture)

        val fakeRequest = authorisedRequestForEori(eori3)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage().url)
      }

      "no undertaking could be found for the eori associated with the request" in {
        mockRetrieveUndertaking(eori)(Option.empty.toFuture)

        val fakeRequest = authorisedRequestForEori(eori)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage().url)
      }
    }

    "throw an error" when {

      "an error occurred retrieving the undertaking" in {
        mockRetrieveUndertaking(eori)(Future.failed(new RuntimeException("Error")))

        val fakeRequest = authorisedRequestForEori(eori)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest)

        a[RuntimeException] shouldBe thrownBy(result.futureValue)
      }
    }
  }

  private def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  private def authorisedRequestForEori(e: EORI) = AuthenticatedEscRequest(
    "Foo",
    "Bar",
    FakeRequest(),
    e
  )

}
