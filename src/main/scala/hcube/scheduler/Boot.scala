package hcube.scheduler

import java.io.File
import java.net.URL

import com.coreos.jetcd.EtcdClientBuilder
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.{EtcdBackend, JobSpecCache, JobSpecShuffle, JsonStorageFormat}

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

    val schedulerConfig = config.getConfig("hcube.scheduler")

    logger.info("Initializing etcd client.")
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints(schedulerConfig.getString("etcd.host"))
      .build()
    val jsonFormat = new JsonStorageFormat

    logger.info("Instantiating etcd backend.")
    val backend = new EtcdBackend(client, jsonFormat)
      with JobSpecShuffle
      with JobSpecCache {
        val lifetimeMillis = schedulerConfig.getInt("backend.cacheLifetimeMillis")
      }

    logger.info("Starting loop scheduler.")
    val scheduler = new LoopScheduler(backend,
      commitSuccess = schedulerConfig.getBoolean("loopScheduler.commitSuccess"))
    scheduler()
  }

  private def logConfiguration(config: Config): Unit = {
    logger.info(s"Environment variables:\n{}", sys.env.mkString("\n"))
    logger.info(s"Typesafe config:\n{}", config.root().render())
  }
}
