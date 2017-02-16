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

  override def apply(): Unit = loop(System.currentTimeMillis())

  private def nextInterval(now: Long): Long = (now / delta) * delta + delta

  /**
    * [t0, t1] - time boundaries for current interval
    * t0 - beginning time of the current interval
    * t1 - end time of the current interval
    * t1 = t0 + delta
    */
  @tailrec private def loop(now: Long): Unit = {
    val t0 = nextInterval(now)
    val t1 = t0 + delta

    val diff = t0 - now
    if (diff > tolerance) {
      // sleep until interval starts
      TimeUtil.sleep(diff)
    }

    processJobs(t0, t1)

    loop(System.currentTimeMillis())
  }

  private def processJobs(t0: Long, t1: Long): Unit = {

  }

}