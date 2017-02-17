package hcube.scheduler

import java.io.File
import java.net.URL

import com.coreos.jetcd.EtcdClientBuilder
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.{EtcdBackend, JobSpecCache, JobSpecShuffle, JsonStorageFormat}
import hcube.scheduler.cleanup.CleanUpTask
import hcube.scheduler.tasks.TaskRunner

import scala.concurrent.ExecutionContext
import scala.sys.SystemProperties

object Boot {

  val logger = Logger(getClass)

  val props = new SystemProperties

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    logger.info("Loading configuration.")

    val rootConfig = props.get("hcube.scheduler.conf-file") match {
      case Some(url: String) => ConfigFactory.systemProperties()
        .withFallback(ConfigFactory.parseURL(new URL(url)))
      case None => ConfigFactory.load("scheduler")
        .withFallback(ConfigFactory.parseFile(new File("conf/scheduler.conf")))
        .withFallback(ConfigFactory.load("default-scheduler"))
    }

    logConfiguration(rootConfig)

    val config = rootConfig.getConfig("hcube.scheduler")

    logger.info("Initializing etcd client.")
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints(config.getString("etcd.host"))
      .build()
    val jsonFormat = new JsonStorageFormat

    logger.info("Instantiating etcd backend.")
    val backend = new EtcdBackend(client, jsonFormat)
      with JobSpecShuffle
      with JobSpecCache {
      val lifetimeMillis = config.getInt("backend.cacheLifetimeMillis")
    }

    val runner = new TaskRunner(1)
    if (!config.getBoolean("cleanUp.disable")) {
      runner.schedule(CleanUpTask(backend), config.getLong("cleanUp.delay"))
    }

    logger.info("Starting loop scheduler.")
    val scheduler = new LoopScheduler(backend,
      commitSuccess = config.getBoolean("loopScheduler.commitSuccess"))
    scheduler()


    val scheduler = SchedulerFactory(config.getConfig("hcube.scheduler"))
    logger.info("Starting scheduler")
    scheduler()

    runner.shutdown()

  }

  private def logConfiguration(config: Config): Unit = {
    logger.info(s"Environment variables:\n{}", sys.env.mkString("\n"))
    logger.info(s"Typesafe config:\n{}", config.root().render())
  }
}
