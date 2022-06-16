import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.24.0",
    "uk.gov.hmrc"       %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "3.21.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "9.6.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28"  % "5.24.0"   % "test",
    "org.jsoup"   %  "jsoup"                   % "1.15.1"   % "test",
    "org.mockito" %  "mockito-core"            % "4.6.1"    % "test",
    "org.mockito" %% "mockito-scala"           % "1.17.7"   % "test",
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.7"   % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
