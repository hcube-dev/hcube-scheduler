package hcube.scheduler.job

import java.time.{Instant, ZonedDateTime}

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.TimeUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process

class SystemExecJob(implicit ec: ExecutionContext) extends Job {

  val logger = Logger(getClass)

  override def apply(time: Long, payload: Map[String, Any]): Unit = {
    logger.debug(s"Executing SystemExecJob, time: ${ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), TimeUtil.UTC)}, payload: $payload")
    payload.get("cmd").foreach { case cmd: String =>
      Future {
        val code = Process(cmd).!
        logger.debug(s"Executed system exec job, code: $code")
      }
    }
  }

}

object SystemExecJob {

  val logger = Logger(getClass)

}
