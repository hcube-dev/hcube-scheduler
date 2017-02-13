package hcube.scheduler.job

import java.time.Instant

trait Job {

  def apply(time: Instant, payload: Map[String, Any])

}
