package hcube.scheduler.job

import java.time.Instant
import java.time.ZonedDateTime._

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.TimeUtil

object LogJob extends Job {

  val logger = Logger(getClass)

  override def apply(time: Long, payload: Map[String, Any]): Unit =
    logger.info(s"Executing LogJob, time: ${ofInstant(Instant.ofEpochMilli(time), TimeUtil.UTC)}, payload: $payload")

}
