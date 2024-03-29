package com.github.tuleism.home.process

import java.net.URI
import java.nio.file.Path

import scalaz.zio.TaskR
import scalaz.zio.blocking.Blocking

object FFMPEG {

  def convertVideoFromPlaylist(m3uURI: URI, output: Path): TaskR[Blocking, ProcessResult] = {
    ProcessRunner.run(Seq("ffmpeg", "-i", m3uURI.toString, "-c", "copy", output.toString))
  }
}
