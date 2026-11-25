/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BeneficiaryInfoRespSpec extends AnyWordSpec with Matchers {

  "BeneficiaryInfoResp.toString" should {

    "mask the EORI and redact the name and ID value" in {
      val resp =
        BeneficiaryInfoResp(Some("GB123456789012"), Some("Joe Bloggs"), Some("NINO"), Some("AB123456C"), Some(false))
      resp.toString shouldBe "BeneficiaryInfoResp(Some(GB12345XXX9012),Some(***),Some(NINO),Some(***),Some(false))"
    }

    "not leak PII when logged inside a BeneficiaryIDResponse" in {
      val resp = BeneficiaryIDResponse(
        Some("2026-11-20"),
        Some(Seq(BeneficiaryInfoResp(Some("GB123456789012"), Some("Joe Bloggs"), None, None, None)))
      )
      val logged = s"Beneficiary ID Response = $resp"
      logged should not include "GB123456789012"
      logged should not include "Joe Bloggs"
    }

    "leave empty fields as None" in {
      BeneficiaryInfoResp(
        None,
        None,
        None,
        None,
        None
      ).toString shouldBe "BeneficiaryInfoResp(None,None,None,None,None)"
    }
  }
}
