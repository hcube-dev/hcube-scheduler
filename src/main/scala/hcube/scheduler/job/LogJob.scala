package hcube.scheduler.job
import java.time.{Instant, ZoneId, ZonedDateTime}

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.TimeUtil

class LogJob extends Job {

  import LogJob._

  override def apply(time: Instant, payload: Map[String, Any]): Unit = {
    val dt = ZonedDateTime.ofInstant(time, TimeUtil.UTC)
    logger.info(s"Executing LogJob, time: $dt, payload: $payload")
  }

}

object LogJob {

  val logger = Logger(getClass)

}
