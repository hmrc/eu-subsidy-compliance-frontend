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

package uk.gov.hmrc.eusubsidycompliancefrontend.forms

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimEoriFormProvider.Fields._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.OptionalClaimEori
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.withGbPrefix
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, undertaking}

class ClaimEoriFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val underTest = new ClaimEoriFormProvider(undertaking)

  "claim eori form validation" must {

    "return no errors for a submission where no EORI was entered" in {
      validateAndCheckSuccess("false", None)
    }

    "return no errors for a submission where an EORI was entered" in {
      validateAndCheckSuccess("true", Some(eori1.drop(2)))
    }

    "return no errors for a submission where an EORI was entered with prefix GB" in {
      validateAndCheckSuccess("true", Some(eori1))
    }

    "return an error if the yes radio button is selected and no eori number is entered" in {
      validateAndCheckError("true", None)(EoriNumber, Required)
    }

    "return an error if the yes radio button is selected and an invalid eori number is entered" in {
      validateAndCheckError("true", Some("sausages"))("claim-eori", IncorrectFormat)
    }

  }

  private def validateAndCheckSuccess(radioButton: String, eoriNumber: Option[String]) = {
    val result = processForm(radioButton, eoriNumber)
    result mustBe Right(OptionalClaimEori(radioButton, eoriNumber.map(withGbPrefix)))
  }

  private def validateAndCheckError(
    radioButton: String,
    eoriNumber: Option[String]
  )(errorField: String, errorMessage: String, args: String*) = {
    val result = processForm(radioButton, eoriNumber)


    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError(errorField, errorMessage, args))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $errorField"
  }

  private def processForm(radioButton: String, eoriNumber: Option[String]) =
    underTest.form.mapping.bind(
      Map(
        YesNoRadioButton -> radioButton,
        EoriNumber -> eoriNumber.getOrElse("")
      )
    )

}
