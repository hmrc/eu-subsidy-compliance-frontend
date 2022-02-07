import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootStrapVersion = "5.18.0"
  val monocleVersion   = "2.1.0"
  val hmrcMongoVersion = "0.59.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootStrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "1.31.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                  % "2.7.0",
    "com.chuusai"       %% "shapeless"                  % "2.3.7"
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootStrapVersion % Test,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % Test,
    "org.jsoup"            % "jsoup"                   % "1.14.3"         % Test,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.36.8"         % "test, it",
    "org.scalatestplus"   %% "mockito-3-4"             % "3.2.7.0"        % "test, it",
    "org.scalamock"       %% "scalamock"               % "5.2.0"          % Test
  )
}
