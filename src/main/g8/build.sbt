val scala3Version = "3.1.0"

// Versions
lazy val logbackVersion         = "1.2.6"
lazy val catsVersion            = "2.6.1"
lazy val catsEffectVersion      = "3.2.9"
lazy val postgresqlVersion      = "42.2.24"
lazy val skunkVersion           = "0.2.2"
lazy val circeVersion           = "0.14.1"
lazy val fs2Version             = "3.1.5"
lazy val flywayVersion          = "8.0.0"
lazy val http4sVersion          = "0.23.5"
lazy val munitVersion           = "0.7.29"
lazy val munitCatsEffectVersion = "1.0.6"

// Environment variables
/*
 * !!!! IMPORTANT !!!!
 * Due to a bug in sbt-dotenv you MUST `reload` sbt before running any flyway commands.
 */
lazy val POSTGRES_HOST     = sys.env.getOrElse("POSTGRES_HOST", "localhost")
lazy val POSTGRES_PORT     = sys.env.getOrElse("POSTGRES_PORT", "5432").toInt
lazy val POSTGRES_DB       = sys.env.getOrElse("POSTGRES_DB", "$name$")
lazy val POSTGRES_USER     = sys.env.getOrElse("POSTGRES_USER", "$name$_owner")
lazy val POSTGRES_PASSWORD = sys.env.getOrElse("POSTGRES_PASSWORD", "")

lazy val root = project
  .in(file("."))
  .enablePlugins(FlywayPlugin)
  .settings(
    flywayUrl      := s"jdbc:postgresql://\${POSTGRES_HOST}:\${POSTGRES_PORT}/\${POSTGRES_DB}",
    flywayUser     := POSTGRES_USER,
    flywayPassword := POSTGRES_PASSWORD,
    flywayLocations += "db/migrations/default"
  )
  .settings(
    name         := "$name$",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:_",
      "-unchecked",
      "-source:future"
    ),
    assembly / assemblyJarName := "$name$.jar",
    assembly / test            := {},
    mainClass                  := Some("$name$.Main")
  )
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic"     % logbackVersion,
      "org.typelevel" %% "cats-core"           % catsVersion,
      "org.typelevel" %% "cats-effect"         % catsEffectVersion,
      "org.postgresql" % "postgresql"          % postgresqlVersion,
      "org.flywaydb"   % "flyway-core"         % flywayVersion,
      "org.tpolecat"  %% "skunk-core"          % skunkVersion,
      "io.circe"      %% "circe-generic"       % circeVersion,
      "co.fs2"        %% "fs2-core"            % fs2Version,
      "co.fs2"        %% "fs2-io"              % fs2Version,
      "org.http4s"    %% "http4s-ember-server" % http4sVersion,
      "org.http4s"    %% "http4s-ember-client" % http4sVersion,
      "org.http4s"    %% "http4s-circe"        % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.scalameta" %% "munit"               % munitVersion           % Test,
      "org.typelevel" %% "munit-cats-effect-3" % munitCatsEffectVersion % Test
    )
  )
  .settings(
    testFrameworks += new TestFramework("munit.Framework"),
    Test / parallelExecution := false
  )
