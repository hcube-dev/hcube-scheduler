package hcube.scheduler

import com.coreos.jetcd.EtcdClientBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.{EtcdBackend, JobSpecCache, JobSpecShuffle, JsonStorageFormat}
import hcube.scheduler.job.Job
import hcube.scheduler.model.JobSpec

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

object SchedulerFactory {

  val logger = Logger(getClass)

  def apply(config: Config)(implicit ec: ExecutionContext): Scheduler = {
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

    logger.info("Adding jobs defined in config.")
    config.getConfigList("jobs")
      .foreach { c =>
        backend
          .put(JobSpec.fromConfig(c))
          .onComplete(r => logger.info(s"Job add status: $r"))
      }

    logger.info("Creating job executor.")
    val jobExecutor = new JobExecutor(
      backend,
      Job.createDefaultDispatcher,
      commitSuccess = config.getBoolean("executor.commitSuccess")
    )

    logger.info("Creating clock.")
    val loopClock = new LoopClock(
      jobExecutor,
      tickPeriod = config.getLong("clock.deltaMillis"),
      tolerance = config.getLong("clock.toleranceMillis"),
      continueOnInterrupt = config.getBoolean("clock.continueOnInterrupt")
    )

    val scheduledClock = new ScheduledClock(
      jobExecutor,
      tickPeriod = config.getLong("clock.deltaMillis")
    )

    logger.info("Creating scheduler.")
    new DefaultScheduler(
      backend,
      scheduledClock,
      config.getBoolean("cleanUp.disable"),
      config.getLong("cleanUp.delayMillis"),
      config.getInt("cleanUp.jobsCount")
    )
  }

}
