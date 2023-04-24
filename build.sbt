import play.sbt.routes.RoutesKeys.routesGenerator
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName: String = "business-customer-frontend"
lazy val appDependencies: Seq[ModuleID] = AppDependencies()

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
val silencerVersion = "1.7.12"
lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages :=
      "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;models.*;views.*",
    ScoverageKeys.coverageMinimumStmtTotal := 99,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
lazy val microservice: Project = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    playSettings,
    routesGenerator := InjectedRoutesGenerator,
    scalaSettings,
    defaultSettings(),
    scoverageSettings,
    scalacOptions += "-Ywarn-unused:-explicits,-implicits",
    scalaVersion := "2.12.15",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    majorVersion := 4,
    Compile / scalacOptions += "-P:silencer:pathFilters=target/.*"
  )
  .settings(
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .settings(
    resolvers += Resolver.jcenterRepo
  )