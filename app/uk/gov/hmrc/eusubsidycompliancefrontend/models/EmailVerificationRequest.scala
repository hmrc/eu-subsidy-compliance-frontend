/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json._

case class EmailVerificationRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  deskproServiceName: Option[String],
  accessibilityStatementUrl: String,
  email: Option[Email],
  lang: Option[String],
  backUrl: Option[String],
  pageTitle: Option[String]
)

case class Email(address: String, enterUrl: String)

object Email {
  implicit val format: Format[Email] = Json.format[Email]
}
object EmailVerificationRequest {
  implicit val writes: OFormat[EmailVerificationRequest] = Json.format[EmailVerificationRequest]

  implicit class EmailVerificationRequestOps(emailVerificationRequest: EmailVerificationRequest) {
    val asJson: JsObject = writes.writes(emailVerificationRequest)
  }

  // This should have its own test, verifyEmail in EmailVerificationService test was just using wildcards so line coverage
  //  is achieved without actually testing the messaging done by parameters
  def createVerifyEmailRequest(
    credId: String,
    redirectVerifyEmailUrl: String,
    email: String,
    backUrl: String
  ): EmailVerificationRequest =
    EmailVerificationRequest(
      credId = credId,
      continueUrl = redirectVerifyEmailUrl,
      origin = "EU Subsidy Compliance",
      deskproServiceName = None,
      accessibilityStatementUrl = "",
      email = Some(
        Email(
          address = email,
          enterUrl = ""
        )
      ),
      lang = None,
      backUrl = Some(backUrl),
      pageTitle = None
    )

}

case class EmailVerificationResponse(redirectUri: String)

object EmailVerificationResponse {
  implicit val formats: OFormat[EmailVerificationResponse] = Json.format[EmailVerificationResponse]
}

case class CompletedEmail(
  emailAddress: String,
  verified: Boolean,
  locked: Boolean
)

object CompletedEmail {
  implicit val reads: Reads[CompletedEmail] = Json.reads[CompletedEmail]
}

case class EmailVerificationStatusResponse(emails: List[CompletedEmail])

object EmailVerificationStatusResponse {
  implicit val reads: Reads[EmailVerificationStatusResponse] = Json.reads[EmailVerificationStatusResponse]
}

sealed trait EmailVerificationStatus
object EmailVerificationStatus {
  case object Verified extends EmailVerificationStatus
  case object Unverified extends EmailVerificationStatus
  case object Locked extends EmailVerificationStatus
  case object Error extends EmailVerificationStatus
}
