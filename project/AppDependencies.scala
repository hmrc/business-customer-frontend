import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0",
    "uk.gov.hmrc" %% "domain" % "5.9.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.9.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.14",
    "uk.gov.hmrc" %% "govuk-template" % "5.54.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % "test",
    "org.jsoup" % "jsoup" % "1.13.1" % "test",
    "org.mockito" % "mockito-core" % "3.3.3" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
