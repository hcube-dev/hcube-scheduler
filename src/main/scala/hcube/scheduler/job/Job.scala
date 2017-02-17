package hcube.scheduler.job

import scala.concurrent.ExecutionContext

trait Job {

  def apply(time: Long, payload: Map[String, Any])

}

object Job {

  def createDefaultDispatcher(implicit ec: ExecutionContext): (String) => Job = {
    val systemExecJob = new SystemExecJob
    (jobType: String) => {
      jobType match {
        case "log" => LogJob
        case "http" => HttpRequestJob
        case "system" => systemExecJob
      }
    }
  }

}
