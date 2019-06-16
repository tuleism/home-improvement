package com.github.tuleism.home.process

import scalaz.zio.blocking.{Blocking, effectBlocking}
import scalaz.zio.{TaskR, ZIO}

import scala.sys.process.Process

object ProcessRunner {

  def run(fullCommand: Seq[String]): TaskR[Blocking, ProcessResult] = {
    val logger = new ProcessLogger()

    ZIO(Process(fullCommand).run(logger)).flatMap { processHandle =>
      effectBlocking(processHandle.exitValue())
        .map((logger.outs, logger.errs, _))
        .onInterrupt {
          ZIO(processHandle.destroy()).orDie
        }
    }
  }
}
