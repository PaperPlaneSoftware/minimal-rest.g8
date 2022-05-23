val scala3Version = "3.1.2"

// Versions
lazy val logbackVersion         = "1.2.11"
lazy val catsVersion            = "2.6.1"
lazy val catsEffectVersion      = "3.2.9"
lazy val postgresqlVersion      = "42.2.25"
lazy val skunkVersion           = "0.2.2"
lazy val circeVersion           = "0.14.1"
lazy val fs2Version             = "3.1.6"
lazy val http4sVersion          = "0.23.11"
lazy val munitVersion           = "0.7.29"
lazy val munitCatsEffectVersion = "1.0.7"
lazy val tydal3Version          = "0.2"

resolvers += "jitpack" at "https://jitpack.io"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "$name;format="space,snake"$",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:_",
      "-unchecked",
      "-source:future"
    ),
    assembly / assemblyJarName := "$name;format="space,snake"$.jar",
    assembly / test            := {},
    mainClass                  := Some("$name;format="space,snake"$.Main")
  )
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback"    % "logback-classic"     % logbackVersion,
      "org.typelevel"    %% "cats-core"           % catsVersion,
      "org.typelevel"    %% "cats-effect"         % catsEffectVersion,
      "org.postgresql"    % "postgresql"          % postgresqlVersion,
      "org.tpolecat"     %% "skunk-core"          % skunkVersion,
      "com.github.epifab" % "tydal3"              % tydal3Version,
      "io.circe"         %% "circe-generic"       % circeVersion,
      "co.fs2"           %% "fs2-core"            % fs2Version,
      "co.fs2"           %% "fs2-io"              % fs2Version,
      "org.http4s"       %% "http4s-ember-server" % http4sVersion,
      "org.http4s"       %% "http4s-ember-client" % http4sVersion,
      "org.http4s"       %% "http4s-circe"        % http4sVersion,
      "org.http4s"       %% "http4s-dsl"          % http4sVersion,
      "org.scalameta"    %% "munit"               % munitVersion           % Test,
      "org.typelevel"    %% "munit-cats-effect-3" % munitCatsEffectVersion % Test
    )
  )
  .settings(
    testFrameworks += new TestFramework("munit.Framework"),
    Test / parallelExecution := false
  )
