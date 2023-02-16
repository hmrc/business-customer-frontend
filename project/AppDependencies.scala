import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.25.0",
    "uk.gov.hmrc"       %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "6.5.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "9.6.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.3"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28"  % "5.25.0"   % "test",
    "org.jsoup"   %  "jsoup"                   % "1.15.3"   % "test",
    "org.mockito" %  "mockito-core"            % "4.7.0"    % "test",
    "org.mockito" %% "mockito-scala"           % "1.17.12"   % "test",
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.12"   % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
