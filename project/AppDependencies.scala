import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.14.0",
    "uk.gov.hmrc" %% "domain" % "5.11.0-play-27",
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "1.17.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-27",
    "com.typesafe.play" %% "play-json-joda" % "2.9.2",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test",
    "org.jsoup" % "jsoup" % "1.13.1" % "test",
    "org.mockito" % "mockito-core" % "3.10.0" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26" % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
