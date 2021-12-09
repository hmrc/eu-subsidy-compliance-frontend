/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.types

import cats.implicits._
import play.api.libs.json._
import shapeless._
import tag._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types

import scala.util.matching.Regex

trait ValidatedType[BaseType] {

  trait Tag

  lazy val className: String = this.getClass.getSimpleName

  def validateAndTransform(in: BaseType): Option[BaseType]

  def apply(in: BaseType): BaseType @@ Tag =
    of(in).getOrElse{
      throw new IllegalArgumentException(
        s""""$in" is not a valid ${className.init}"""
      )
    }

  def of(in: BaseType): Option[BaseType @@ Tag] =
    validateAndTransform(in) map {
      x => tag[Tag][BaseType](x)
    }
}

class RegexValidatedString(
  val regex: String,
  transform: String => String = identity
) extends ValidatedType[String] {

  val regexCompiled: Regex = regex.r

  def validateAndTransform(in: String): Option[String] =
    transform(in).some.filter(regexCompiled.findFirstIn(_).isDefined)
}

trait SimpleJson {

  def validatedStringFormat(
    A: ValidatedType[String],
    name: String
  ): Format[@@[String, A.Tag]] = new Format[String @@ A.Tag] {

    override def reads(
      json: JsValue
    ): JsResult[String @@ A.Tag] = json match {
      case JsString(value) =>
        A.validateAndTransform(value) match {
          case Some(v) => JsSuccess(A(v))
          case None => JsError(s"Expected a valid $name, got $value instead")
        }
      case xs: JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $xs instead""")))
    }

    override def writes(
      o: String @@ A.Tag
    ): JsValue = JsString(o)
  }


  def validatedBigDecimalFormat(
    A: ValidatedType[BigDecimal],
    name: String
  ): Format[@@[BigDecimal, A.Tag]] = new Format[BigDecimal @@ A.Tag] {
    override def reads(json: JsValue): JsResult[BigDecimal @@ A.Tag] = {
      json match {
        case JsNumber(value) =>
          A.validateAndTransform(value) match {
            case Some(v) => JsSuccess(A(v))
            case None => JsError(s"Expected a valid $name, got $value instead.")
          }
        case xs: JsValue => JsError(
          JsPath -> JsonValidationError(Seq(s"""Expected a valid IndustrySectorLimit, got $xs instead"""))
        )
      }
    }

    override def writes(o: BigDecimal @@ A.Tag): JsValue = JsNumber(BigDecimal(o.toString))
  }

  implicit val sectorLimitFormat: Format[@@[BigDecimal, types.IndustrySectorLimit.Tag]] =
    validatedBigDecimalFormat(IndustrySectorLimit, "IndustrySectorLimit")

  implicit val positiveSubsidyAmountFormat: Format[@@[BigDecimal, types.PositiveSubsidyAmount.Tag]] =
    validatedBigDecimalFormat(PositiveSubsidyAmount, "PositiveSubsidyAmount")

  implicit val subsidyAmountFormat: Format[@@[BigDecimal, types.SubsidyAmount.Tag]] =
    validatedBigDecimalFormat(SubsidyAmount, "SubsidyAmount")

  implicit val phonenumberFormat: Format[@@[String, types.PhoneNumber.Tag]] =
    validatedStringFormat(PhoneNumber, "phonenumber")

  implicit val eisParamValueFormat: Format[@@[String, types.EisParamValue.Tag]] =
    validatedStringFormat(EisParamValue, "paramValue")

  implicit val eisStatusStringFormat: Format[@@[String, types.EisStatusString.Tag]] =
    validatedStringFormat(EisStatusString, "eisStatusString")

  implicit val undertakingRefFormat: Format[@@[String, types.UndertakingRef.Tag]] =
    validatedStringFormat(UndertakingRef, "undertakingReference")

  implicit val undertakingNameFormat: Format[@@[String, types.UndertakingName.Tag]] =
    validatedStringFormat(UndertakingName, "undertakingName")

  implicit val industrySectorFormat: Format[@@[String, types.Sector.Tag]] =
    validatedStringFormat(Sector, "industrySector")

  implicit val eoriFormat: Format[@@[String, types.EORI.Tag]] =
    validatedStringFormat(EORI, "eori")

  implicit val subsidyRefFormat: Format[@@[String, types.SubsidyRef.Tag]] =
    validatedStringFormat(SubsidyRef, "subsidyRef")

  implicit val amendmentTypeFormat: Format[@@[String, types.EisSubsidyAmendmentType.Tag]] =
    validatedStringFormat(EisSubsidyAmendmentType, "amendmentType")

  implicit val traderRefFormat: Format[@@[String, types.TraderRef.Tag]] =
    validatedStringFormat(TraderRef, "traderRef")

  implicit val declarationIDFormat: Format[@@[String, types.DeclarationID.Tag]] =
    validatedStringFormat(DeclarationID, "declarationId")

  implicit val taxTypeFormat: Format[@@[String, types.TaxType.Tag]] =
    validatedStringFormat(TaxType, "taxType")

}