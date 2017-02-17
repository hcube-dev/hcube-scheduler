package hcube.scheduler

import java.io.File
import java.net.URL

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext
import scala.sys.SystemProperties

object Boot {

  val logger = Logger(getClass)

  val props = new SystemProperties

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    logger.info("Loading configuration.")
    val config = props.get("hcube.scheduler.conf-file") match {
      case Some(url: String) => ConfigFactory.systemProperties()
        .withFallback(ConfigFactory.parseURL(new URL(url)))
      case None => ConfigFactory.load("scheduler")
        .withFallback(ConfigFactory.parseFile(new File("conf/scheduler.conf")))
    }
    logConfiguration(config)

    val scheduler = SchedulerFactory(config.getConfig("hcube.scheduler"))
    logger.info("Starting scheduler")
    scheduler()
  }

  private def logConfiguration(config: Config): Unit = {
    logger.info(s"Environment variables:\n{}", sys.env.mkString("\n"))
    logger.info(s"Typesafe config:\n{}", config.root().render())
  }

}
