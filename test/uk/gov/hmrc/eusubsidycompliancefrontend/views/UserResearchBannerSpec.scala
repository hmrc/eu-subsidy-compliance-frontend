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

package uk.gov.hmrc.eusubsidycompliancefrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.PlaySupport
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import java.time.LocalDate

class UserResearchBannerSpec extends PlaySupport {

  private val request = FakeRequest()
  private val dueDate = LocalDate.of(2026, 12, 1)

  private lazy val undertakingDisabledPage: UndertakingDisabledPage =
    fakeApplication.injector.instanceOf[UndertakingDisabledPage]
  private lazy val claimConfirmationPage: ClaimConfirmationPage =
    fakeApplication.injector.instanceOf[ClaimConfirmationPage]
  private lazy val noClaimConfirmationPage: NoClaimConfirmationPage =
    fakeApplication.injector.instanceOf[NoClaimConfirmationPage]
  private lazy val becomeAdminConfirmationPage: BecomeAdminConfirmationPage =
    fakeApplication.injector.instanceOf[BecomeAdminConfirmationPage]
  private lazy val signOutPage: SignOutPage = fakeApplication.injector.instanceOf[SignOutPage]
  private lazy val confirmationPage: ConfirmationPage = fakeApplication.injector.instanceOf[ConfirmationPage]

  private def parse(html: Html): Document = Jsoup.parse(html.body)

  private val endPages: Seq[(String, () => Document)] = Seq(
    "Undertaking disabled page" -> (() => parse(undertakingDisabledPage()(request, messages, appConfig))),
    "Claim confirmation page" -> (() =>
      parse(claimConfirmationPage(dueDate, isSuspended = true)(request, messages, appConfig))
    ),
    "No claim confirmation page" -> (() =>
      parse(noClaimConfirmationPage(dueDate, isSuspended = true)(request, messages, appConfig))
    ),
    "Become admin confirmation page" -> (() => parse(becomeAdminConfirmationPage()(request, messages, appConfig))),
    "Leave undertaking signed out page" -> (() => parse(signOutPage()(request, messages, appConfig)))
  )

  endPages.foreach { case (name, render) =>
    s"The $name" should {

      lazy val document = render()
      lazy val banner = document.select(".hmrc-user-research-banner")

      "display the linear user research banner" in {
        banner.size() shouldBe 1
      }

      "use the platform banner content" in {
        banner.select(".hmrc-user-research-banner__title").text() shouldBe "You may receive a voucher for feedback"
        banner.select(".hmrc-user-research-banner__link").text() shouldBe "Join our research panel (opens in new tab)"
      }

      "link to the EUSC research url" in {
        banner.select(".hmrc-user-research-banner__link").attr("href") shouldBe appConfig.userResearchUrl
      }

      "not display a hide button" in {
        banner.select(".hmrc-user-research-banner__close").size() shouldBe 0
      }

      "not display the old before you go research link" in {
        document.select("#research-link").size() shouldBe 0
        document.select("#researchLinkText").size() shouldBe 0
      }
    }
  }

  "A non end page" should {
    "not display the user research banner" in {
      val document = parse(
        confirmationPage(UndertakingRef("UR123456"), EORI("GB922456789077"), intentToAddBusiness = true)(
          request,
          messages,
          appConfig
        )
      )
      document.select(".hmrc-user-research-banner").size() shouldBe 0
    }
  }
}
