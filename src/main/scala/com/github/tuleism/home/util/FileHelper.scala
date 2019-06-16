package com.github.tuleism.home.util

import java.nio.file.{Files, Path}

import scalaz.zio.ZManaged
import scalaz.zio.blocking._

object FileHelper {

  def temporaryFile(suffix: Option[String] = None): ZManaged[Blocking, Throwable, Path] = {
    ZManaged.make {
      effectBlocking {
        Files.createTempFile(null, suffix.orNull)
      }
    }(tempPath => effectBlocking(tempPath.toFile.delete()).orDie)
  }
}
