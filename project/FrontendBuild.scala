import sbt._
object FrontendBuild extends Build with MicroService {
  val appName = "business-customer-frontend"
  override lazy val appDependencies: Seq[ModuleID] = appSpecificDependencies.all
  private val frontendBootstrapVersion = "12.9.0"
  private val httpCachingClientVersion = "8.3.0"
  private val playPartialsVersion = "6.5.0"
  private val domainVersion = "5.3.0"
  private val hmrcTestVersion = "3.9.0-play-25"
  private val scalaTestPlusVersion = "2.0.1"
  object appSpecificDependencies {
    import play.sbt.PlayImport.ws
    val compile = Seq(
      ws,
      "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
      "uk.gov.hmrc" %% "domain" % domainVersion,
      "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
      "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
      "uk.gov.hmrc" %% "auth-client" % "2.4.0"
    )
    val test = Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test",
      "org.jsoup" % "jsoup" % "1.11.3" % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test",
      "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % "test"
    )
    val all = compile ++ test
  }
}
