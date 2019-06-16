package com.github.tuleism.home.crawler

import java.net.URI
import java.nio.file.{Files, Paths}

import com.github.tuleism.home.process.FFMPEG
import scalaz.zio.blocking.Blocking
import scalaz.zio.console._
import scalaz.zio.{TaskR, ZIO}

object VideoDownloader {

  def download(streamUrl: String, videoTitle: String, directory: String): TaskR[Console with Blocking, Unit] = {
    for {
      uri        <- ZIO(new URI(streamUrl))
      dirPath    <- ZIO(Paths.get(directory))
      fullPath   <- ZIO(dirPath.resolve(videoTitle + ".mkv"))
      downloaded <- ZIO(Files.exists(fullPath))
      _          <- putStrLn(s"$fullPath already existed, skipping!").when(downloaded)
      _ <- FFMPEG
        .convertVideoFromPlaylist(uri, fullPath)
        .flatMap { result =>
          putStrLn(result._1.mkString("\n")).when(result._2 != 0)
        }
        .when(!downloaded)
    } yield ()
  }

  def downloadMultiple(mapping: Map[String, String], directory: String): TaskR[Console with Blocking, Unit] = {
    for {
      _ <- ZIO.foreachParN(4L)(mapping) {
        case (videoTitle, streamUrl) => download(streamUrl, videoTitle, directory)
      }
    } yield ()
  }
}
