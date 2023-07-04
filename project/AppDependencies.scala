import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.25.0",
    "uk.gov.hmrc"       %% "domain"                     % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.14.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "10.0.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28"  % "5.25.0"   % "test",
    "org.jsoup"   %  "jsoup"                   % "1.15.4"   % "test",
    "org.mockito" %  "mockito-core"            % "4.11.0"    % "test",
    "org.mockito" %% "mockito-scala"           % "1.17.14"   % "test",
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.14"   % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
