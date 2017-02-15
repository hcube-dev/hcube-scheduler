package hcube.scheduler.job
import java.time.Instant
import java.time.ZonedDateTime._

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.TimeUtil

class LogJob extends Job {

  import LogJob._

  override def apply(time: Instant, payload: Map[String, Any]): Unit = {
    logger.info(s"Executing LogJob, time: ${ofInstant(time, TimeUtil.UTC)}, payload: $payload")
  }

}

object LogJob {

  val logger = Logger(getClass)

}
