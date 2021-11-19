package $name$

import cats.effect.{ExitCode, IO, IOApp}
import natchez.Trace.Implicits.noop

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] = Server.serve.as(ExitCode.Success)
