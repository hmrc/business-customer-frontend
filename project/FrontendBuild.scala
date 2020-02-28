import sbt._

object FrontendBuild extends Build with MicroService {

  override lazy val appDependencies: Seq[ModuleID] = appSpecificDependencies.all

  val appName = "business-customer-frontend"

  object appSpecificDependencies {
    import play.sbt.PlayImport.ws
    val compile: Seq[ModuleID] = Seq(
      ws,
      "uk.gov.hmrc" %% "bootstrap-play-26" % "1.4.0",
      "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
      "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-26",
      "uk.gov.hmrc" %% "play-ui" % "8.8.0-play-26",
      "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
      "uk.gov.hmrc" %% "auth-client" % "2.33.0-play-26",
      "com.typesafe.play" %% "play-json-joda" % "2.6.13",
      "uk.gov.hmrc" %% "govuk-template" % "5.52.0-play-26"
    )
    val test: Seq[ModuleID] = Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % "test",
      "org.jsoup" % "jsoup" % "1.12.2" % "test",
      "org.mockito" % "mockito-core" % "3.3.1" % "test",
      "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % "test"
    )
    val all: Seq[ModuleID] = compile ++ test
  }
}
