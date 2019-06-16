package com.github.tuleism.home.crawler

import java.util.concurrent.TimeUnit

import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import scalaz.zio.blocking._
import scalaz.zio.console._
import scalaz.zio.{blocking => _, _}

import scala.collection.JavaConverters._

object ConfluentVideo extends App {

  val summitPageUrls = List(
    "https://www.confluent.io/resources/kafka-summit-new-york-2019",
    "https://www.confluent.io/resources/kafka-summit-san-francisco-2018",
    "https://www.confluent.io/resources/kafka-summit-london-2019"
  )

  override def run(args: List[String]) = {
    managedDrivers(3)
      .use { drivers =>
        for {
          queue <- Queue.bounded[ChromeDriver](drivers.size)
          _     <- queue.offerAll(drivers)
          videoPageUrls <- ZIO
            .foreachPar(summitPageUrls) { summitPageUrl =>
              withPool(queue)(getVideoPageUrls(_, summitPageUrl))
            }
            .map(_.flatten)
          _ <- putStrLn("Page urls:")
          _ <- ZIO.foreach(videoPageUrls)(putStrLn)
          mapping <- ZIO
            .foreachPar(videoPageUrls) { videoPageUrl =>
              withPool(queue)(getVideoInfo(_, videoPageUrl).option)
            }
            .map(_.flatten.toMap)
          _ <- putStrLn("All links:")
          _ <- ZIO.foreach(mapping) {
            case (title, url) => putStrLn(s"$title -> $url")
          }
        } yield mapping
      }
      .flatMap { mapping =>
        VideoDownloader.downloadMultiple(mapping, "/run/media/tule/Elements/courses/talks/confluent/london2019")
      }
      .map(_ => 0)
      .orDie
  }

  def getVideoPageUrls(driver: ChromeDriver, summitPageUrl: String): TaskR[Blocking, List[String]] = {
    effectBlocking {
      driver.get(summitPageUrl)
      driver
        .findElementsByCssSelector("a[target]")
        .iterator()
        .asScala
        .toList
        .filter(_.getText.contains("View Video and Slides"))
        .map(_.getAttribute("href"))
    }
  }

  def getVideoInfo(driver: ChromeDriver, videoPageUrl: String): TaskR[Blocking, (String, String)] = {
    effectBlocking {
      // implicitly wait for elements to appear
      driver.get(videoPageUrl)
      val videoTitle = videoPageUrl.split("/").filter(_.nonEmpty).last
      driver.switchTo().frame(driver.findElementByCssSelector(".vidyard_iframe"))
      val videoStreamElements =
        driver.findElementsByCssSelector("video > source[data-res]").iterator().asScala.toList
      val videoStreamUrl = videoStreamElements
        .find(_.getAttribute("data-res").contains("1080"))
        .getOrElse(videoStreamElements.find(_.getAttribute("data-res").contains("720")).get)
        .getAttribute("src")
      (videoTitle, videoStreamUrl)
    }
  }

  private def managedDrivers(n: Int): Managed[Throwable, List[ChromeDriver]] = {
    Managed.foreach(1 to n)(
      _ =>
        Managed.make(
          ZIO {
            val chromeOptions = new ChromeOptions()
            chromeOptions.addArguments("--headless")
            val driver = new ChromeDriver(chromeOptions)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
            driver
          }
        )(driver => ZIO(driver.close()).orDie)
    )
  }

  private def withPool[R, E, I, O](queue: Queue[I])(f: I => ZIO[R, E, O]): ZIO[R, E, O] = {
    queue.take.bracket(queue.offer)(f)
  }
}
