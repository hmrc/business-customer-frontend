/*
 * Copyright 2025 HM Revenue & Customs
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

import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys.routesGenerator
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName: String = "business-customer-frontend"

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)
lazy val playSettings: Seq[Setting[?]] = Seq.empty
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
  .enablePlugins(plugins *)
  .settings(
    playSettings,
    routesGenerator := InjectedRoutesGenerator,
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
    scalaSettings,
    defaultSettings(),
    scoverageSettings,
    scalacOptions += "-Ywarn-unused:-explicits,-implicits",
    scalaVersion := "2.13.16",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    retrieveManaged := true,
    majorVersion := 4,
    Compile/scalacOptions ++= Seq(
      "-Wconf:src=target/.*:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s"
    )
  )
  .settings(

    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .disablePlugins(JUnitXmlReportPlugin)
