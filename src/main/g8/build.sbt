import sbt.Keys.testFrameworks

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
lazy val POSTGRES_DB       = sys.env.getOrElse("POSTGRES_DB", "$name;format="camel"$")
lazy val POSTGRES_USER     = sys.env.getOrElse("POSTGRES_USER", "$name;format="camel"$_owner")
lazy val POSTGRES_PASSWORD = sys.env.getOrElse("POSTGRES_PASSWORD", "")

ThisBuild / organization := "$organization$"
ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.0.2"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Ykind-projector:underscores",
    "-source:future"
  ),
  evictionErrorLevel := Level.Warn,
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic"     % logbackVersion,
    "org.scalameta" %% "munit"               % munitVersion           % Test,
    "org.typelevel" %% "munit-cats-effect-3" % munitCatsEffectVersion % Test
  ),
  testFrameworks += new TestFramework("munit.Framework"),
  Test / parallelExecution := false
)

lazy val domain = (project in file("00-domain"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )

lazy val persistence = (project in file("01-persistence"))
  .dependsOn(domain)
  .settings(commonSettings)

lazy val persistence_pg = (project in file("02-persistence-pg"))
  .dependsOn(domain, persistence)
  .enablePlugins(FlywayPlugin)
  .settings(commonSettings)
  .settings(
    flywayUrl      := s"jdbc:postgresql://\${POSTGRES_HOST}:\${POSTGRES_PORT}/\${POSTGRES_DB}",
    flywayUser     := POSTGRES_USER,
    flywayPassword := POSTGRES_PASSWORD,
    flywayLocations += "db/migration"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "org.tpolecat"  %% "skunk-core" % skunkVersion
    )
  )

lazy val service = (project in file("02-service"))
  .dependsOn(domain, persistence)
  .settings(commonSettings)

lazy val app = (project in file("03-app"))
  .dependsOn(domain, persistence, persistence_pg, service)
  .settings(commonSettings)
  .settings(
    mainClass                  := Some("$package$.app.Main"),
    assembly / assemblyJarName := "$name$.jar",
    test in assembly           := {},
    libraryDependencies ++= Seq(
      "io.circe"    %% "circe-generic"       % circeVersion,
      "co.fs2"      %% "fs2-core"            % fs2Version,
      "co.fs2"      %% "fs2-io"              % fs2Version,
      "org.http4s"  %% "http4s-ember-server" % http4sVersion,
      "org.http4s"  %% "http4s-ember-client" % http4sVersion,
      "org.http4s"  %% "http4s-circe"        % http4sVersion,
      "org.http4s"  %% "http4s-dsl"          % http4sVersion,
      "org.flywaydb" % "flyway-core"         % flywayVersion
    )
  )
