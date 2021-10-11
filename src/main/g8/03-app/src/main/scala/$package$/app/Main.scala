package $package$
package app

import cats.effect.*

object Main extends IOApp:
  def run(args: List[String]) = Server.serve[IO].as(ExitCode.Success)
