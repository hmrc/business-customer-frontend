import sbt.*

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"  %% "bootstrap-frontend-play-30"   % "9.5.0",
    "uk.gov.hmrc"  %% "domain-play-30"               % "10.0.0",
    "uk.gov.hmrc"  %% "play-partials-play-30"        % "10.0.0",
    "uk.gov.hmrc"  %% "play-frontend-hmrc-play-30"   % "11.5.0",
    "uk.gov.hmrc" %% "http-caching-client-play-30"   % "12.1.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % "9.5.0"    % Test
  )
  val itDependencies: Seq[ModuleID] = Seq()
}
