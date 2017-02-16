package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.utils.TimeUtil

import scala.annotation.tailrec

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler(
  backend: Backend,
  delta: Long = 1000,
  tolerance: Long = 50
) extends Scheduler {

  override def apply(): Unit = {
    val now = System.currentTimeMillis()
    val t0 = (now / delta) * delta
    loop(t0)
  }

  /**
    * [t0, t1] - time boundaries for current interval
    * t0 - beginning time of the current interval
    * t1 - end time of the current interval
    * @param t0
    */
  @tailrec private def loop(t0: Long): Unit = {
    val now = System.currentTimeMillis()
    val t1 = t0 + delta

    if (now - t0 > tolerance) {
      // sleep till next interval
      TimeUtil.sleep(t1 - now)
    }

    // TODO process jobs

    loop(t1)
  }

}