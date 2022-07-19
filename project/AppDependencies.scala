import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootStrapVersion = "5.23.0"
  val hmrcMongoVersion = "0.63.0"
  val enumeratumVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootStrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "3.14.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                     % "2.7.0",
    "ai.x"              %% "play-json-extensions"          % "0.42.0",
    "com.chuusai"       %% "shapeless"                     % "2.3.9",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.11.0-play-28",
    "com.beachape"      %% "enumeratum"                    % enumeratumVersion,
    "com.beachape"      %% "enumeratum-play-json"          % enumeratumVersion
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootStrapVersion % "test, it",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % "test, it",
    "org.jsoup"            % "jsoup"                   % "1.14.3"         % Test,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.36.8"         % "test, it",
    "org.scalatestplus"   %% "mockito-3-4"             % "3.2.10.0"       % "test, it",
    "org.scalamock"       %% "scalamock"               % "5.2.0"          % Test
  )
}
