package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.utils.TimeUtil

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler(
  backend: Backend,
  interval: Long
) extends Scheduler {

  override def apply(): Unit = {
    val now = System.currentTimeMillis()
    val intervalStart = (now / interval) * interval
    if (now - intervalStart > 0) {
      TimeUtil.sleep((intervalStart + interval) - now)
    }

    while(true) {
      val now = System.currentTimeMillis()
      val start = now / interval

      val jobs = Await.result(backend.pullJobs(), Duration.Inf)

      val t1 = System.currentTimeMillis()
    }

  }

}