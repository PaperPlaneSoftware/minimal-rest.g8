package $name;format="space,snake"$

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] = Server.serve.as(ExitCode.Success)
