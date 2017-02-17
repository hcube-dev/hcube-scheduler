package hcube.scheduler

import com.coreos.jetcd.EtcdClientBuilder
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.{EtcdBackend, JobSpecCache, JobSpecShuffle, JsonStorageFormat}

import scala.concurrent.ExecutionContext

object Boot {

  val logger = Logger(getClass)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    logger.info("Initializing etcd client")
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints("http://hcube-etcd-v3-1-0-node-0:2379")
      .build()
    val jsonFormat = new JsonStorageFormat

    logger.info("Instantiating etcd backend")
    val backend = new EtcdBackend(client, jsonFormat)
      with JobSpecShuffle
      with JobSpecCache { val lifetimeMillis = 5000 }

    logger.info("Starting loop scheduler")
    val scheduler = new LoopScheduler(backend)
    scheduler()
  }

}
