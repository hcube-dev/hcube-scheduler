package hcube.scheduler.job

import java.time.Instant

class HttpRequestJob extends Job {

  override def apply(time: Instant, payload: Map[String, Any]): Unit = ???

}
