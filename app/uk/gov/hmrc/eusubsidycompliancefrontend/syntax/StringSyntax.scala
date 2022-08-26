package uk.gov.hmrc.eusubsidycompliancefrontend.syntax

object StringSyntax {

  private def True = "true"

  implicit class StringOps(val s: String) extends AnyVal {
    def isTrue: Boolean = s == True
  }

}
