/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.implicits.catsSyntaxOptionId
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, EscService, Store, UndertakingJourney}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{undertaking, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountControllerSpec  extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour {
  val mockEscService = mock[EscService]

  override def overrideBindings           = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  val controller = instanceOf[AccountController]


  def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  "AccountControllerSpec" when {

    "handling request to get Account page" must {

      def performAction() = controller.getAccountPage(FakeRequest())

      behave like authBehaviour(() => performAction())

      "display the page" when {

        def test(undertaking: Undertaking) = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Right(undertaking))
            checkPageIsDisplayed(
              performAction(),
              messageFromMessageKey("account.title", undertaking.name),
              {doc =>
                val htmlBody = doc.select(".govuk-grid-column-one-third").html()
                htmlBody should include regex messageFromMessageKey(
                  "account-homepage.cards.card1.link1",
                  routes.SubsidyController.getReportPayment().url
                )
                if(undertaking.undertakingBusinessEntity.length > 1)
                  htmlBody should include regex messageFromMessageKey(
                    "account-homepage.cards.card3.link1View",
                    routes.BusinessEntityController.getAddBusinessEntity().url
                  )
                else
                  htmlBody should include regex messageFromMessageKey(
                    "account-homepage.cards.card3.link1Add",
                    routes.BusinessEntityController.getAddBusinessEntity().url
                  )

              }
            )

          }
        }

        "there is a view link on the page" in {
          test(undertaking)
        }

        "there is a add link on the page" in {
          test(undertaking.copy(undertakingBusinessEntity = List(businessEntity1)))
        }

      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "there is error in retrieving the undertaking" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching eligibility journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing eligibility journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(None))
            mockPut[EligibilityJourney](EligibilityJourney(), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in retrieving  undertaking  journey data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing  undertaking  journey data" in {

          val undertakingData = UndertakingJourney.fromUndertakingOpt(None)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(None))
            mockPut[UndertakingJourney](undertakingData, eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in fetching Business entity  journey data" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }


        "there is an error in storing Business entity  journey data" in {
          val businessEntityData = BusinessEntityJourney.fromUndertakingOpt(None)

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(None))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(None))
            mockPut[BusinessEntityJourney](businessEntityData, eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "there is an error in storing Undertaking data" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking.some))
            mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            mockPut[Undertaking](undertaking, eori1)(Left(Error(exception)))

          }
          assertThrows[Exception](await(performAction()))

        }
      }

      "redirect to next page" when {

        "There is no existing retrieve undertaking" when {

          "eligibility Journey  is not complete and undertaking Journey is blank" in {
            inSequence {
              mockAuthWithNecessaryEnrolment()
              mockRetreiveUndertaking(eori1)(Future.successful(None))
              mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyNotComplete.some))
              mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
              mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            }
            checkIsRedirect(performAction(), routes.EligibilityController.firstEmptyPage())
          }

          "eligibility Journey  is complete and undertaking Journey is not complete" in {
            inSequence {
              mockAuthWithNecessaryEnrolment()
              mockRetreiveUndertaking(eori1)(Future.successful(None))
              mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
              mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
              mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            }
            checkIsRedirect(performAction(), routes.UndertakingController.firstEmptyPage())
          }

          "eligibility Journey  and undertaking Journey are  complete" in {
            inSequence {
              mockAuthWithNecessaryEnrolment()
              mockRetreiveUndertaking(eori1)(Future.successful(None))
              mockGet[EligibilityJourney](eori1)(Right(eligibilityJourneyComplete.some))
              mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
              mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
            }
            checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity())
          }
        }

      }

    }

  }

}