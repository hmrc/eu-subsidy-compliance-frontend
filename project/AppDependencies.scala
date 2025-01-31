import sbt._

object AppDependencies {

  val bootStrapVersion = "9.7.0"
  val hmrcMongoVersion = "2.4.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootStrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % "11.11.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "org.typelevel" %% "cats-core" % "2.13.0",
    "com.chuusai" %% "shapeless" % "2.3.12",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "3.2.0",
    "com.beachape" %% "enumeratum" % "1.7.5",
    "com.beachape" %% "enumeratum-play-json" % "1.8.2"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootStrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.jsoup" % "jsoup" % "1.18.3" % Test,
    "org.scalamock" %% "scalamock" % "6.1.1" % Test
  )
}
