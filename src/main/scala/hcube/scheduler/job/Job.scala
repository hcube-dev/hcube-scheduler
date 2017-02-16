package hcube.scheduler.job

trait Job {

  def apply(time: Long, payload: Map[String, Any])

}

object Job {

  def apply(jobType: String): Job = {
    jobType match {
      case "log" => LogJob
      case "http" => HttpRequestJob
      case "system" => SystemExecJob
    }
  }

}
