import sbt._

object AppDependencies {
  import play.sbt.PlayImport.ws

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.18.0",
    "uk.gov.hmrc"       %% "domain"                     % "6.2.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.2.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "1.31.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "9.5.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28"  % "5.18.0"  % "test",
    "org.jsoup"   %  "jsoup"                   % "1.14.3"  % "test",
    "org.mockito" %  "mockito-core"            % "4.2.0"   % "test",
    "org.mockito" %% "mockito-scala"           % "1.16.49" % "test",
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.49" % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
