/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Format
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

import scala.concurrent.Future
import scala.reflect.ClassTag

class NACECheckDetailsControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore)
  )

  private val controller = instanceOf[NACECheckDetailsController]
  private val exception = new Exception("Test exception")

  "NACECheckDetailsController" when {

    "handling request to get check details" must {

      def performAction() = controller.getCheckDetails()(
        FakeRequest(GET, routes.NACECheckDetailsController.getCheckDetails().url)
      )

      "handle errors gracefully" when {

        "call to get or create undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            (mockJourneyStore
              .getOrCreate(_: UndertakingJourney)(
                _: ClassTag[UndertakingJourney],
                _: EORI,
                _: Format[UndertakingJourney]
              ))
              .expects(*, *, eori1, *)
              .returning(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "redirect to sector selection page" when {

        "sector value is empty" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(None)
          )
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(Right(journey.copy(isNaceCYA = true)))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getSector.url)
        }

        "sector value is too short (less than 5 characters)" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.cropAnimalProduction.some)
          )
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(Right(journey.copy(isNaceCYA = true)))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getSector.url)
        }

        "sector value has exactly 4 characters" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.meat.some)
          )
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(Right(journey.copy(isNaceCYA = true)))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getSector.url)
        }
      }

      "display the check details page" when {

        "sector value is valid - retail code (47.11)" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(Right(journey.copy(isNaceCYA = true)))
          }
          val result = performAction()
          status(result) shouldBe OK
        }
      }
    }

    "handling request to post check details" must {

      def performAction(data: (String, String)*) = controller.postCheckDetails()(
        FakeRequest(POST, routes.NACECheckDetailsController.postCheckDetails.url)
          .withFormUrlEncodedBody(data: _*)
      )

      "handle errors gracefully" when {

        "call to get or create undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            (mockJourneyStore
              .getOrCreate(_: UndertakingJourney)(
                _: ClassTag[UndertakingJourney],
                _: EORI,
                _: Format[UndertakingJourney]
              ))
              .expects(*, *, eori1, *)
              .returning(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("confirmDetails" -> "true")))
        }
      }

      "return bad request with form errors" when {

        "no confirmDetails value is provided with valid naceCode" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )
          mockAuthWithEnrolmentAndNoEmailVerification()
          mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))

          val result = performAction("naceCode" -> "47.11")
          status(result) shouldBe BAD_REQUEST
        }
      }

      "redirect to next page" when {

        "user confirms details in normal journey (not amend, not UpdateNaceMode)" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some),
            isAmend = false,
            mode = ""
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(isNaceCYA = false, isAmend = false))
            )
          }

          checkIsRedirect(
            performAction("confirmDetails" -> "true"),
            routes.UndertakingController.getAddBusiness.url
          )
        }

        "user confirms details in amend journey" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some),
            isAmend = true,
            mode = ""
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(isNaceCYA = false, isAmend = false))
            )
          }

          checkIsRedirect(
            performAction("confirmDetails" -> "true"),
            routes.UndertakingController.postAmendUndertaking.url
          )
        }

        "user confirms details when mode is UpdateNaceMode" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some),
            isAmend = false,
            mode = "UpdateNaceMode"
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(isNaceCYA = false, isAmend = false))
            )
          }

          checkIsRedirect(
            performAction("confirmDetails" -> "true"),
            routes.UndertakingController.postAmendUndertaking.url
          )
        }

        "user confirms details with isAmend = false but mode = UpdateNaceMode" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some),
            isAmend = false,
            mode = "UpdateNaceMode"
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(isNaceCYA = false, isAmend = false))
            )
          }

          checkIsRedirect(
            performAction("confirmDetails" -> "true"),
            routes.UndertakingController.postAmendUndertaking.url
          )
        }

        "user rejects details" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.other.some)))
            )
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(internalNaceCode = "", isNaceCYA = false))
            )
          }

          checkIsRedirect(
            performAction("confirmDetails" -> "false"),
            routes.UndertakingController.getSector.url
          )
        }

        "user rejects details with different sector" in {
          val journey = UndertakingJourney(
            sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(sector = UndertakingJourney.Forms.UndertakingSectorFormPage(Sector.other.some)))
            )
            mockUpdate[UndertakingJourney](eori1)(
              Right(journey.copy(internalNaceCode = "", isNaceCYA = false))
            )
          }
          checkIsRedirect(
            performAction("confirmDetails" -> "false"),
            routes.UndertakingController.getSector.url
          )
        }
      }
    }

    "deriving level 1 code from level 2 code" must {

      "return A for agriculture codes" in {
        controller.deriveLevel1Code("01") shouldBe "A"
        controller.deriveLevel1Code("02") shouldBe "A"
        controller.deriveLevel1Code("03") shouldBe "A"
      }

      "return B for mining codes" in {
        controller.deriveLevel1Code("05") shouldBe "B"
        controller.deriveLevel1Code("06") shouldBe "B"
        controller.deriveLevel1Code("07") shouldBe "B"
        controller.deriveLevel1Code("08") shouldBe "B"
        controller.deriveLevel1Code("09") shouldBe "B"
      }

      "return C for manufacturing codes" in {
        controller.deriveLevel1Code("10") shouldBe "C"
        controller.deriveLevel1Code("11") shouldBe "C"
        controller.deriveLevel1Code("12") shouldBe "C"
        controller.deriveLevel1Code("13") shouldBe "C"
        controller.deriveLevel1Code("14") shouldBe "C"
        controller.deriveLevel1Code("15") shouldBe "C"
        controller.deriveLevel1Code("16") shouldBe "C"
        controller.deriveLevel1Code("17") shouldBe "C"
        controller.deriveLevel1Code("18") shouldBe "C"
        controller.deriveLevel1Code("19") shouldBe "C"
        controller.deriveLevel1Code("20") shouldBe "C"
        controller.deriveLevel1Code("21") shouldBe "C"
        controller.deriveLevel1Code("22") shouldBe "C"
        controller.deriveLevel1Code("23") shouldBe "C"
        controller.deriveLevel1Code("24") shouldBe "C"
        controller.deriveLevel1Code("25") shouldBe "C"
        controller.deriveLevel1Code("26") shouldBe "C"
        controller.deriveLevel1Code("27") shouldBe "C"
        controller.deriveLevel1Code("28") shouldBe "C"
        controller.deriveLevel1Code("29") shouldBe "C"
        controller.deriveLevel1Code("30") shouldBe "C"
        controller.deriveLevel1Code("31") shouldBe "C"
        controller.deriveLevel1Code("32") shouldBe "C"
        controller.deriveLevel1Code("33") shouldBe "C"
      }

      "return D for electricity codes" in {
        controller.deriveLevel1Code("35") shouldBe "D"
      }

      "return E for water supply codes" in {
        controller.deriveLevel1Code("36") shouldBe "E"
        controller.deriveLevel1Code("37") shouldBe "E"
        controller.deriveLevel1Code("38") shouldBe "E"
        controller.deriveLevel1Code("39") shouldBe "E"
      }

      "return F for construction codes" in {
        controller.deriveLevel1Code("41") shouldBe "F"
        controller.deriveLevel1Code("42") shouldBe "F"
        controller.deriveLevel1Code("43") shouldBe "F"
      }

      "return G for wholesale and retail codes" in {
        controller.deriveLevel1Code("46") shouldBe "G"
        controller.deriveLevel1Code("47") shouldBe "G"
      }

      "return H for transport codes" in {
        controller.deriveLevel1Code("49") shouldBe "H"
        controller.deriveLevel1Code("50") shouldBe "H"
        controller.deriveLevel1Code("51") shouldBe "H"
        controller.deriveLevel1Code("52") shouldBe "H"
        controller.deriveLevel1Code("53") shouldBe "H"
      }

      "return I for accommodation codes" in {
        controller.deriveLevel1Code("55") shouldBe "I"
        controller.deriveLevel1Code("56") shouldBe "I"
      }

      "return J for publishing codes" in {
        controller.deriveLevel1Code("58") shouldBe "J"
        controller.deriveLevel1Code("59") shouldBe "J"
        controller.deriveLevel1Code("60") shouldBe "J"
      }

      "return K for telecommunications codes" in {
        controller.deriveLevel1Code("61") shouldBe "K"
        controller.deriveLevel1Code("62") shouldBe "K"
        controller.deriveLevel1Code("63") shouldBe "K"
      }

      "return L for financial codes" in {
        controller.deriveLevel1Code("64") shouldBe "L"
        controller.deriveLevel1Code("65") shouldBe "L"
        controller.deriveLevel1Code("66") shouldBe "L"
      }

      "return M for real estate codes" in {
        controller.deriveLevel1Code("68") shouldBe "M"
      }

      "return N for professional codes" in {
        controller.deriveLevel1Code("69") shouldBe "N"
        controller.deriveLevel1Code("70") shouldBe "N"
        controller.deriveLevel1Code("71") shouldBe "N"
        controller.deriveLevel1Code("72") shouldBe "N"
        controller.deriveLevel1Code("73") shouldBe "N"
        controller.deriveLevel1Code("74") shouldBe "N"
        controller.deriveLevel1Code("75") shouldBe "N"
      }

      "return O for administrative codes" in {
        controller.deriveLevel1Code("77") shouldBe "O"
        controller.deriveLevel1Code("78") shouldBe "O"
        controller.deriveLevel1Code("79") shouldBe "O"
        controller.deriveLevel1Code("80") shouldBe "O"
        controller.deriveLevel1Code("81") shouldBe "O"
        controller.deriveLevel1Code("82") shouldBe "O"
      }

      "return P for public administration codes" in {
        controller.deriveLevel1Code("84") shouldBe "P"
      }

      "return Q for education codes" in {
        controller.deriveLevel1Code("85") shouldBe "Q"
      }

      "return R for health codes" in {
        controller.deriveLevel1Code("86") shouldBe "R"
        controller.deriveLevel1Code("87") shouldBe "R"
        controller.deriveLevel1Code("88") shouldBe "R"
      }

      "return S for arts codes" in {
        controller.deriveLevel1Code("90") shouldBe "S"
        controller.deriveLevel1Code("91") shouldBe "S"
        controller.deriveLevel1Code("92") shouldBe "S"
        controller.deriveLevel1Code("93") shouldBe "S"
      }

      "return T for other service codes" in {
        controller.deriveLevel1Code("94") shouldBe "T"
        controller.deriveLevel1Code("95") shouldBe "T"
        controller.deriveLevel1Code("96") shouldBe "T"
      }

      "return U for households codes" in {
        controller.deriveLevel1Code("97") shouldBe "U"
        controller.deriveLevel1Code("98") shouldBe "U"
      }

      "return V for extraterritorial codes" in {
        controller.deriveLevel1Code("99") shouldBe "V"
      }
    }

    "building NACE view model" must {

      "correctly derive all NACE levels for a retail code (47.11)" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

        viewModel.naceLevel4Code shouldBe "47.11"
        viewModel.naceLevel3Code shouldBe "47.1"
        viewModel.naceLevel2Code shouldBe "47"
        viewModel.naceLevel1Code shouldBe "G"
        viewModel.sector shouldBe Sector.other
      }

      "set showLevel1 to true for non-agriculture sectors" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.showLevel1 shouldBe true
      }

      "set showLevel1_1 correctly for manufacturing sector (C)" in {
        val naceCode = "10.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.naceLevel1Code shouldBe "C"
        viewModel.showLevel1_1 shouldBe true
      }

      "set showLevel1_1 to false for manufacturing code 32" in {
        val naceCode = "32.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.naceLevel1Code shouldBe "C"
        viewModel.showLevel1_1 shouldBe false
      }

      "set showLevel1_1 to false for manufacturing code 33" in {
        val naceCode = "33.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.showLevel1_1 shouldBe false
      }

      "set showLevel2 to true for other codes" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.showLevel2 shouldBe true
      }

      "set showLevel2 to false for public administration code 84" in {
        val naceCode = "84"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.showLevel2 shouldBe false
      }

      "set showLevel3 to true when level3 code does not end with .0" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.naceLevel3Code shouldBe "47.1"
        viewModel.showLevel3 shouldBe true
      }

      "set showLevel4 to true when last digits don't match" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.showLevel4 shouldBe true
      }

      "generate correct change URLs" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

        viewModel.changeSectorUrl shouldBe routes.UndertakingController.getSector.url
        viewModel.changeLevel1Url should not be empty
        viewModel.changeLevel2Url should not be empty
        viewModel.changeLevel3Url should not be empty
        viewModel.changeLevel4Url should not be empty
      }
    }

    "generating level 2 change URLs" must {

      "return correct URL for manufacturing (C)" in {
        val naceCode = "10.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for construction (F)" in {
        val naceCode = "41"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for retail (G)" in {
        val naceCode = "47.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for accommodation (I)" in {
        val naceCode = "55.1"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for publishing (J)" in {
        val naceCode = "58.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for administrative (O)" in {
        val naceCode = "77.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for health (R)" in {
        val naceCode = "86.1"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for households (U)" in {
        val naceCode = "97"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for transport (H)" in {
        val naceCode = "49.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for mining (B)" in {
        val naceCode = "05.10"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for professional (N)" in {
        val naceCode = "69.1"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for water supply (E)" in {
        val naceCode = "36"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for telecommunications (K)" in {
        val naceCode = "61.1"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for arts (S)" in {
        val naceCode = "90.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }

      "return correct URL for financial (L)" in {
        val naceCode = "64.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url should not be empty
      }
    }

    "testing getLevel1_1ChangeUrl for manufacturing subcategories" must {

      "return correct URL for clothes/textiles/homeware - textiles (13)" in {
        val naceCode = "13.10"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
      }

      "return correct URL for clothes/textiles/homeware - leather (15)" in {
        val naceCode = "15.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
      }

      "return correct URL for clothes/textiles/homeware - wood (16)" in {
        val naceCode = "16.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
      }

      "return correct URL for metals/chemicals/materials - rubber/plastic (22)" in {
        val naceCode = "22.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
      }

      "return correct URL for clothes/textiles/homeware - furniture (31)" in {
        val naceCode = "31"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
      }

      "return correct URL for computers/electronics/machinery - computers (26)" in {
        val naceCode = "26.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController
          .loadComputersElectronicsMachineryPage()
          .url
      }

      "return correct URL for computers/electronics/machinery - electrical (27)" in {
        val naceCode = "27.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController
          .loadComputersElectronicsMachineryPage()
          .url
      }

      "return correct URL for computers/electronics/machinery - other machinery (28)" in {
        val naceCode = "28.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController
          .loadComputersElectronicsMachineryPage()
          .url
      }

      "return correct URL for food/beverages/tobacco - food (10)" in {
        val naceCode = "10.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url
      }

      "return correct URL for food/beverages/tobacco - tobacco (12)" in {
        val naceCode = "12"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url
      }

      "return correct URL for metals/chemicals/materials - chemicals (20)" in {
        val naceCode = "20.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
      }

      "return correct URL for metals/chemicals/materials - non-metallic (23)" in {
        val naceCode = "23.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
      }

      "return correct URL for metals/chemicals/materials - fabricated metal (25)" in {
        val naceCode = "25.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
      }

      "return correct URL for paper/printed - paper (17)" in {
        val naceCode = "17.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url
      }

      "return correct URL for paper/printed - printing (18)" in {
        val naceCode = "18.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url
      }

      "return correct URL for vehicles/transport - other transport (30)" in {
        val naceCode = "30.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadVehiclesTransportPage().url
      }

      "return default URL for other manufacturing (32)" in {
        val naceCode = "32.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
      }

      "return default URL for repair/maintenance (33)" in {
        val naceCode = "33.11"
        val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
        viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
      }
    }
  }
}
