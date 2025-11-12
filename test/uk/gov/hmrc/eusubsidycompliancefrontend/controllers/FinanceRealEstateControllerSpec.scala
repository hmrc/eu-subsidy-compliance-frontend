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

import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig

class FinanceRealEstateControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[FinanceRealEstateController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val Intermediation = "43.6"
    val OtherFeeContract = "68.32"
    val BuyingSelling = "68.11"
    val DevelopmentProjects = "68.12"
    val FeeContract = "68.3"
    val PropertyDevelopment = "68.1"
    val realEstateRental4 = "68.20"
    val AuxiliaryFinancialServices = "66.1"
    val AuxiliaryInsurance = "66.2"
    val fundManagementActivities = "66.30"
    val InsuranceAgents = "66.22"
    val RiskEvaluation = "66.21"
    val OtherAuxiliaryInsurance = "66.29"
    val FinancialMarkets = "66.11"
    val SecurityBrokerage = "66.12"
    val OtherAuxiliaryFinancial = "66.19"
    val FinancialServices = "64"
    val InsuranceReinsurancePensionFunding = "65"
    val auxiliaryActivities = "66"
    val MonetaryIntermediation = "64.1"
    val HoldingCompanies = "64.2"
    val TrustsFunds = "64.3"
    val OtherFinancial = "64.9"
    val FinancingConduits = "64.22"
    val HoldingCompaniesFinal = "64.21"
    val InvestmentFunds = "64.31"
    val TrustEstate = "64.32"
    val FinancialLeasing = "64.91"
    val OtherCredit = "64.92"
    val OtherFinancialServices = "64.99"
    val CentralBanking = "64.11"
    val OtherMonetary = "64.19"
    val LifeInsurance = "65.11"
    val NonLifeInsurance = "65.12"
    val InsuranceServices = "65.1"
    val PensionFunding4 = "65.30"
    val Reinsurance4 = "65.20"
  }

  import SectorCodes._

  "FinanceRealEstateController" should {
    "loadFeeContractLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFeeContractLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadFeeContractLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-intermediation", "Intermediation service activities for real estate"),
          ("sector-label-other-fee-contract", "Other real estate activities on a fee or contract basis")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFeeContractLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (Intermediation, navigator.nextPage(Intermediation, "").url),
          (OtherFeeContract, navigator.nextPage(OtherFeeContract, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFeeContractLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitFeeContractLvl4Page().url
              )
                .withFormUrlEncodedBody("feeContract4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitFeeContractLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitFeeContractLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of real estate activities your undertaking does on a fee or contract basis"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPropertyDevelopmentLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPropertyDevelopmentLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadPropertyDevelopmentLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-buying-selling", "Buying and selling of own real estate"),
          ("sector-label-development-projects", "Development of building projects")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPropertyDevelopmentLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (BuyingSelling, navigator.nextPage(BuyingSelling, "").url),
          (DevelopmentProjects, navigator.nextPage(DevelopmentProjects, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPropertyDevelopmentLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitPropertyDevelopmentLvl4Page().url
              )
                .withFormUrlEncodedBody("propertyDev4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitPropertyDevelopmentLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitPropertyDevelopmentLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in property and development"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadRealEstateLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRealEstateLvl3Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadRealEstateLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-fee-contract", "Real estate activities on a fee or contract basis"),
          (
            "sector-label-property-development",
            "Real estate activities with own property and development of building projects"
          ),
          ("sector-label-rental-operating", "Rental and operating of own or leased real estate")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitRealEstateLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (FeeContract, navigator.nextPage(FeeContract, "").url),
          (PropertyDevelopment, navigator.nextPage(PropertyDevelopment, "").url),
          (realEstateRental4, navigator.nextPage(realEstateRental4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRealEstateLvl3Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitRealEstateLvl3Page().url
              )
                .withFormUrlEncodedBody("realEstate3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitRealEstateLvl3Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitRealEstateLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in real estate"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAuxiliaryFinancialLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAuxiliaryFinancialLvl3Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadAuxiliaryFinancialLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-auxiliary-financial-services",
            "Activities auxiliary to financial services (except insurance and pension funding)"
          ),
          ("sector-label-auxiliary-insurance", "Activities auxiliary to insurance and pension funding"),
          ("sector-label-fund-management", "Fund management")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAuxiliaryFinancialLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (AuxiliaryFinancialServices, navigator.nextPage(AuxiliaryFinancialServices, "").url),
          (AuxiliaryInsurance, navigator.nextPage(AuxiliaryInsurance, "").url),
          (fundManagementActivities, navigator.nextPage(fundManagementActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAuxiliaryFinancialLvl3Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitAuxiliaryFinancialLvl3Page().url
              )
                .withFormUrlEncodedBody("auxFinance3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitAuxiliaryFinancialLvl3Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitAuxiliaryFinancialLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in auxiliary financial activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAuxiliaryInsuranceLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAuxiliaryInsuranceLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadAuxiliaryInsuranceLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-insurance-agents", "Activities of insurance agents and brokers"),
          ("sector-label-risk-evaluation", "Risk and damage evaluation"),
          ("sector-label-other-auxiliary-insurance", "Other activities auxiliary to insurance and pension funding")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAuxiliaryInsuranceLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (InsuranceAgents, navigator.nextPage(InsuranceAgents, "").url),
          (RiskEvaluation, navigator.nextPage(RiskEvaluation, "").url),
          (OtherAuxiliaryInsurance, navigator.nextPage(OtherAuxiliaryInsurance, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAuxiliaryInsuranceLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitAuxiliaryInsuranceLvl4Page().url
              )
                .withFormUrlEncodedBody("auxInsurance4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitAuxiliaryInsuranceLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitAuxiliaryInsuranceLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of auxiliary insurance and pension funding activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAuxiliaryNonInsuranceLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAuxiliaryNonInsuranceLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadAuxiliaryNonInsuranceLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-financial-markets", "Administration of financial markets"),
          ("sector-label-security-brokerage", "Security and commodity contracts brokerage"),
          (
            "sector-label-other-auxiliary-financial",
            "Other activities auxiliary to financial services (except insurance and pension funding)"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAuxiliaryNonInsuranceLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (FinancialMarkets, navigator.nextPage(FinancialMarkets, "").url),
          (SecurityBrokerage, navigator.nextPage(SecurityBrokerage, "").url),
          (OtherAuxiliaryFinancial, navigator.nextPage(OtherAuxiliaryFinancial, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAuxiliaryNonInsuranceLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitAuxiliaryNonInsuranceLvl4Page().url
              )
                .withFormUrlEncodedBody("auxNonInsurance4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitAuxiliaryNonInsuranceLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitAuxiliaryNonInsuranceLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of auxiliary financial services activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFinanceInsuranceLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFinanceInsuranceLvl2Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadFinanceInsuranceLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-financial-services", "Financial services (except insurance and pension funding)"),
          ("sector-label-insurance", "Insurance, reinsurance and pension funding"),
          ("sector-label-auxiliary", "Activities auxiliary to financial services and insurance activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFinanceInsuranceLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (FinancialServices, navigator.nextPage(FinancialServices, "").url),
          (InsuranceReinsurancePensionFunding, navigator.nextPage(InsuranceReinsurancePensionFunding, "").url),
          (auxiliaryActivities, navigator.nextPage(auxiliaryActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFinanceInsuranceLvl2Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitFinanceInsuranceLvl2Page().url
              )
                .withFormUrlEncodedBody("finance2" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitFinanceInsuranceLvl2Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitFinanceInsuranceLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking's main business activity in financial services and insurance"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFinancialServicesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFinancialServicesLvl3Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadFinancialServicesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-monetary-intermediation", "Monetary intermediation"),
          ("sector-label-holding-companies", "Holding companies and financing conduits"),
          ("sector-label-trusts-funds", "Trusts, funds and similar financial entities"),
          ("sector-label-other-financial", "Other financial services (except insurance and pension funding)")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFinancialServicesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (MonetaryIntermediation, navigator.nextPage(MonetaryIntermediation, "").url),
          (HoldingCompanies, navigator.nextPage(HoldingCompanies, "").url),
          (TrustsFunds, navigator.nextPage(TrustsFunds, "").url),
          (OtherFinancial, navigator.nextPage(OtherFinancial, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFinancialServicesLvl3Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitFinancialServicesLvl3Page().url
              )
                .withFormUrlEncodedBody("financial3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitFinancialServicesLvl3Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitFinancialServicesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in financial services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHoldingCompaniesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHoldingCompaniesLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadHoldingCompaniesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-financing-conduits", "Financing conduits"),
          ("sector-label-holding-companies-final", "Holding companies")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHoldingCompaniesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (FinancingConduits, navigator.nextPage(FinancingConduits, "").url),
          (HoldingCompaniesFinal, navigator.nextPage(HoldingCompaniesFinal, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHoldingCompaniesLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitHoldingCompaniesLvl4Page().url
              )
                .withFormUrlEncodedBody("holding4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitHoldingCompaniesLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitHoldingCompaniesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in holding companies and financing conduits"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTrustsFundsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTrustsFundsLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadTrustsFundsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-investment-funds", "Money market and non-money market investment funds"),
          ("sector-label-trust-estate", "Trust, estate and agency accounts")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTrustsFundsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (InvestmentFunds, navigator.nextPage(InvestmentFunds, "").url),
          (TrustEstate, navigator.nextPage(TrustEstate, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTrustsFundsLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitTrustsFundsLvl4Page().url
              )
                .withFormUrlEncodedBody("trusts4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitTrustsFundsLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitTrustsFundsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in trusts, funds and similar financial entities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherFinancialLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherFinancialLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadOtherFinancialLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-financial-leasing", "Financial leasing"),
          ("sector-label-other-credit", "Other credit granting"),
          (
            "sector-label-other-financial-services",
            "Other financial service activities (except insurance and pension funding)"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherFinancialLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (FinancialLeasing, navigator.nextPage(FinancialLeasing, "").url),
          (OtherCredit, navigator.nextPage(OtherCredit, "").url),
          (OtherFinancialServices, navigator.nextPage(OtherFinancialServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherFinancialLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitOtherFinancialLvl4Page().url
              )
                .withFormUrlEncodedBody("otherFinance4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitOtherFinancialLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitOtherFinancialLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in other financial services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMonetaryIntermediationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMonetaryIntermediationLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadMonetaryIntermediationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-central-banking", "Central banking"),
          ("sector-label-other-monetary", "Other monetary intermediation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMonetaryIntermediationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (CentralBanking, navigator.nextPage(CentralBanking, "").url),
          (OtherMonetary, navigator.nextPage(OtherMonetary, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMonetaryIntermediationLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitMonetaryIntermediationLvl4Page().url
              )
                .withFormUrlEncodedBody("monetary4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitMonetaryIntermediationLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitMonetaryIntermediationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of monetary intermediation service your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadInsuranceTypeLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadInsuranceTypeLvl4Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadInsuranceTypeLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-life-insurance", "Life insurance"),
          ("sector-label-non-life-insurance", "Non-life insurance")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitInsuranceTypeLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (LifeInsurance, navigator.nextPage(LifeInsurance, "").url),
          (NonLifeInsurance, navigator.nextPage(NonLifeInsurance, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitInsuranceTypeLvl4Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitInsuranceTypeLvl4Page().url
              )
                .withFormUrlEncodedBody("insurance4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitInsuranceTypeLvl4Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitInsuranceTypeLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of insurance your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadInsuranceLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadInsuranceLvl3Page()(
            FakeRequest(GET, routes.FinanceRealEstateController.loadInsuranceLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-insurance", "Insurance"),
          ("sector-label-pension-funding", "Pension funding"),
          ("sector-label-reinsurance", "Reinsurance")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitInsuranceLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (InsuranceServices, navigator.nextPage(InsuranceServices, "").url),
          (PensionFunding4, navigator.nextPage(PensionFunding4, "").url),
          (Reinsurance4, navigator.nextPage(Reinsurance4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitInsuranceLvl3Page()(
              FakeRequest(
                POST,
                routes.FinanceRealEstateController.submitInsuranceLvl3Page().url
              )
                .withFormUrlEncodedBody("insurance3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitInsuranceLvl3Page()(
            FakeRequest(POST, routes.FinanceRealEstateController.submitInsuranceLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in insurance, reinsurance and pension funding"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
