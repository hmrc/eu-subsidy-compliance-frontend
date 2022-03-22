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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.OptionalEORI
import ClaimEoriFormProvider.Fields._
import utils.CommonTestData.{eori1, undertaking}

class ClaimEoriFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val underTest = new ClaimEoriFormProvider(undertaking)

  "claim eori form validation" must {

    "return no errors for a submission where no EORI was entered" in {
      validateAndCheckSucess("false", None)
    }

    "return no errors for a submission where an EORI was entered" in {
      validateAndCheckSucess("true", Some(eori1.drop(2)))
    }

    "return an error if no fields are selected" in {
      validateAndCheckError("", None)(YesNoRadioButton, "error.should-claim-eori.required")
    }

    "return an error if the yes radio button is selected and no eori number is entered" in {
      validateAndCheckError("true", None)(EoriNumber, "error.required")
    }

    "return an error if the yes radio button is selected and an invalid eori number is entered" in {
      validateAndCheckError("true", Some("sausages"))("claim-eori", "error.format")
    }

    "return an error if the entered eori is not part of the undertaking" in {
      validateAndCheckError("true", Some("171717171717"))(EoriNumber, "error.not-in-undertaking")
    }

  }

  private def validateAndCheckSucess(radioButton: String, eoriNumber: Option[String]) = {
    val result = processForm(radioButton, eoriNumber)
    result mustBe Right(OptionalEORI(radioButton, eoriNumber.map(e => s"GB$e")))
  }

  private def validateAndCheckError(radioButton: String, eoriNumber: Option[String])(errorField: String, errorMessage: String, args: String*) = {
    val result = processForm(radioButton, eoriNumber)

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError(errorField, s"$errorMessage", args))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $errorField"
  }

  private def processForm(radioButton: String, eoriNumber: Option[String]) = {
    underTest.form.mapping.bind(
      Map(
        YesNoRadioButton -> radioButton,
        EoriNumber -> eoriNumber.getOrElse(""),
      )
    )
  }

}
