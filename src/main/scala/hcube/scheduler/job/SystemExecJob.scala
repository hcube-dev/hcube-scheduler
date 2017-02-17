package hcube.scheduler.job

import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process

class SystemExecJob(implicit ec: ExecutionContext) extends Job {

  val logger = Logger(getClass)

  override def apply(time: Long, payload: Map[String, Any]): Unit = {
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
