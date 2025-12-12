import sbt.*

object AppDependencies {

  val bootStrapVersion = "10.4.0"
  val hmrcMongoVersion = "2.11.0"

  val compile = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30"                 % bootStrapVersion,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30"                 % "12.24.0",
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"                         % hmrcMongoVersion,
    "org.typelevel"         %% "cats-core"                                  % "2.13.0",
    "com.chuusai"           %% "shapeless"                                  % "2.3.13",
    "uk.gov.hmrc"           %% "play-conditional-form-mapping-play-30"      % "3.4.0",
    "com.beachape"          %% "enumeratum"                                 % "1.9.1",
    "com.beachape"          %% "enumeratum-play-json"                       % "1.9.1"
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"         % bootStrapVersion % Test,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"        % hmrcMongoVersion % Test,
    "org.jsoup"                   % "jsoup"                           % "1.21.2" % Test,
    "org.scalamock"               %% "scalamock"                      % "7.5.2" % Test
  )
}