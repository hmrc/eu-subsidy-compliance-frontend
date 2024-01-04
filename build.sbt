import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import scoverage.ScoverageKeys

val appName = "eu-subsidy-compliance-frontend"

val silencerVersion = "1.7.12"

PlayKeys.playDefaultPort := 9093

val routesSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.eusubsidycompliancefrontend.models.types._",
    "uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus._"
  )
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.12",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Assets / pipelineStages := Seq(gzip),
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:src=target/.*:s"
  )
  .configs(IntegrationTest)
  .settings(routesSettings: _*)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;.*config.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*testonly.*;.*models.*;.*views.*;.*job.*;.*connectors.EuropaConnector;.*connectors.ProxiedHttpClient;.*controllers.test.*;.*journeys.NilReturnJourney;.*journeys.UndertakingJourney;.*util.TimeProvider",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val testSettings = Seq(
  fork := true,
  javaOptions ++= Seq(
    // Uncomment this to use a separate conf for tests
    // "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)

//Check both integration and normal scopes so formatAndTest can be applied when needed more easily.
Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

IntegrationTest / test := (IntegrationTest / test)
  .dependsOn(scalafmtCheckAll)
  .value

addCommandAlias("precommit", ";scalafmt;test:scalafmt;it:test::scalafmt;coverage;test;it:test;coverageReport")
