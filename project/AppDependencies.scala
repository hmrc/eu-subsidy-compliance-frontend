import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootStrapVersion = "8.3.0"
  val hmrcMongoVersion = "1.7.0"
  val enumeratumVersion = "1.7.3"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootStrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % "8.2.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "org.typelevel" %% "cats-core" % "2.9.0",
    "com.chuusai" %% "shapeless" % "2.3.10",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "com.beachape" %% "enumeratum" % enumeratumVersion,
    "com.beachape" %% "enumeratum-play-json" % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootStrapVersion % "test, it",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % "test, it",
    "org.jsoup" % "jsoup" % "1.15.4" % Test,
    "org.scalamock" %% "scalamock" % "5.2.0" % Test
  )
}
