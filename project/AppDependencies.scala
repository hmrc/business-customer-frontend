import sbt.*

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"  %% "bootstrap-frontend-play-30"   % "8.4.0",
    "uk.gov.hmrc"  %% "domain-play-30"               % "9.0.0",
    "uk.gov.hmrc"  %% "play-partials-play-30"        % "9.1.0",
    "uk.gov.hmrc"  %% "play-frontend-hmrc-play-30"   % "8.5.0",
    "uk.gov.hmrc"  %% "http-caching-client-play-30"  % "11.2.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % "8.4.0"    % Test,
    "org.jsoup"         %  "jsoup"                   % "1.17.2"   % Test,
    "org.mockito"       %  "mockito-core"            % "5.10.0"   % Test,
    "org.mockito"       %% "mockito-scala"           % "1.17.30"  % Test,
    "org.mockito"       %% "mockito-scala-scalatest" % "1.17.30"  % Test,
    "org.scalatestplus" %% "scalacheck-1-17"         % "3.2.18.0" % Test
  )
}
