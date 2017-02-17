package hcube.scheduler

import com.coreos.jetcd.EtcdClientBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.{EtcdBackend, JobSpecCache, JobSpecShuffle, JsonStorageFormat}
import hcube.scheduler.job.Job

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

    logger.info("Creating loop scheduler.")
    new LoopScheduler(backend, Job.createDefaultDispatcher,
      commitSuccess = config.getBoolean("loopScheduler.commitSuccess"))
  }

}
