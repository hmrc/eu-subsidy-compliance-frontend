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

import play.api.libs.json.{Format, Json}
import play.api.mvc.{PathBindable, QueryStringBindable}
import shapeless.tag.@@

import scala.util.Try

package object types extends SimpleJson {

  val MaxInputValue = 99999999999.99

  type IndustrySectorLimit = BigDecimal @@ IndustrySectorLimit.Tag
  object IndustrySectorLimit extends ValidatedType[BigDecimal] {
    override def validateAndTransform(in: BigDecimal): Option[BigDecimal] =
      Some(in).filter { x =>
        (x <= MaxInputValue) && (x.scale <= 2)
      }
  }

  type PositiveSubsidyAmount = BigDecimal @@ PositiveSubsidyAmount.Tag
  object PositiveSubsidyAmount extends ValidatedType[BigDecimal] {
    override def validateAndTransform(in: BigDecimal): Option[BigDecimal] =
      Some(in).filter { x =>
        (x >= 0) && (x <= MaxInputValue) && (x.scale <= 2)
      }
  }

  type SubsidyAmount = BigDecimal @@ SubsidyAmount.Tag
  object SubsidyAmount extends ValidatedType[BigDecimal] {
    override def validateAndTransform(in: BigDecimal): Option[BigDecimal] =
      Some(in).filter { x =>
        (x >= -MaxInputValue) && (x <= MaxInputValue) && (x.scale <= 2)
      }
    val Zero: SubsidyAmount = SubsidyAmount(BigDecimal(0))
  }

  type DeclarationID = String @@ DeclarationID.Tag
  object DeclarationID
      extends RegexValidatedString(
        regex = """.{1,18}"""
      )

  type TaxType = String @@ TaxType.Tag
  object TaxType
      extends RegexValidatedString(
        regex = """.{0,3}"""
      )

  type TraderRef = String @@ TraderRef.Tag
  object TraderRef
      extends RegexValidatedString(
        regex = """[A-Za-z0-9\W_ ]{1,400}""" //dev agreed setting an arbitary limit of 400
      )

  type UndertakingName = String @@ UndertakingName.Tag
  object UndertakingName
      extends RegexValidatedString(
        regex = """.{1,105}"""
      )

  type EORI = String @@ EORI.Tag
  object EORI
      extends RegexValidatedString(
        """^(gb|Gb|gB|GB|XI)[0-9]{12,15}$"""
      ) {

    val ValidLengthsWithPrefix = Set(14, 17) // Valid lengths with 2 letter prefix

    def withGbPrefix(eori: String): String =
      if (eori.startsWith("GB")) eori
      else s"GB$eori"

    def formatEori(eori: String): String = {
      val s = eori.replaceAll(" ", "")
      s.take(2).toUpperCase + s.drop(2)
    }
  }

  type UndertakingRef = String @@ UndertakingRef.Tag
  object UndertakingRef
      extends RegexValidatedString(
        regex = """[A-Za-z0-9]{1,17}""" // n.b. no longer exact match for spec which accepts n of any char
      )

  object Sector extends Enumeration {
    type Sector = Value
    val other: types.Sector.Value = Value("0")
    val transport: types.Sector.Value = Value("1")
    val agriculture: types.Sector.Value = Value("2")
    val aquaculture: types.Sector.Value = Value("3")
    implicit val format: Format[Sector] = Json.formatEnum(Sector)
  }
  object UndertakingStatus extends Enumeration {
    type UndertakingStatus = Value
    val active: types.UndertakingStatus.Value = Value(0)
    val suspendedAutomated: types.UndertakingStatus.Value = Value(1)
    val suspendedManual: types.UndertakingStatus.Value = Value(5)
    val inactive: types.UndertakingStatus.Value = Value(9)
    implicit val format: Format[UndertakingStatus] = Json.formatEnum(UndertakingStatus)
  }

  type SubsidyRef = String @@ SubsidyRef.Tag
  object SubsidyRef
      extends RegexValidatedString(
        "^[A-Za-z0-9]{1,10}$" // n.b. no longer exact match for spec which accepts n of any char
      )

  type EisSubsidyAmendmentType = String @@ EisSubsidyAmendmentType.Tag
  object EisSubsidyAmendmentType
      extends RegexValidatedString(
        regex = "1|2|3"
      )

  object AmendmentType extends Enumeration {
    type AmendmentType = Value

    val add: types.AmendmentType.Value = Value("1")
    val amend: types.AmendmentType.Value = Value("2")
    val delete: types.AmendmentType.Value = Value("3")

    implicit val format: Format[AmendmentType] = Json.formatEnum(AmendmentType)

  }

  type ErrorMessage = String @@ ErrorMessage.Tag
  object ErrorMessage
      extends RegexValidatedString(
        """.{1,255}"""
      )

  object EmailStatus extends Enumeration {
    type EmailStatus = Value
    val Unverified: types.EmailStatus.Value = Value("unverified")
    val New: types.EmailStatus.Value = Value("new")
    val Amend: types.EmailStatus.Value = Value("amend")
    val BecomeLead: types.EmailStatus.Value = Value("become-lead")

    implicit val format: Format[EmailStatus] = Json.formatEnum(EmailStatus)

    implicit object Binder extends PathBindable[EmailStatus] {

      override def bind(key: String, value: String): Either[String, EmailStatus] =
        Try(EmailStatus.withName(value)).toOption
          .map(Right(_))
          .getOrElse(Left("Cannot parse parameter '" + key + "' as EmailStatus"))

      override def unbind(key: String, value: EmailStatus): String = value.toString

    }

    implicit val queryStringBindable: QueryStringBindable[EmailStatus] =
      new QueryStringBindable[EmailStatus] {
        override def bind(
          key: String,
          params: Map[String, Seq[String]]
        ): Option[Either[String, EmailStatus]] =
          params.get(key).collect { case Seq(s) =>
            EmailStatus.values.find(_.toString == s).toRight("invalid sort field parameter")
          }

        override def unbind(key: String, value: EmailStatus): String =
          implicitly[QueryStringBindable[String]].unbind(key, value.toString)
      }
  }

  object VerifiedStatus extends Enumeration {
    type VerifiedStatus = Value
    val Verified: types.VerifiedStatus.Value = Value("verified")

    implicit val format: Format[VerifiedStatus] = Json.formatEnum(VerifiedStatus)
  }

}
