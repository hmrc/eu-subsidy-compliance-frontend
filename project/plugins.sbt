resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)
resolvers += Resolver.typesafeRepo("releases")


addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "3.20.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.4.0")
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn" % "0.1.3")
