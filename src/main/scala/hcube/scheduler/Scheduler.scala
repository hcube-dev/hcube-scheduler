package hcube.scheduler

import java.time.Instant

import hcube.scheduler.backend.Backend
import hcube.scheduler.model.JobSpec
import hcube.scheduler.utils.TimeUtil
import hcube.scheduler.utils.TimeUtil.TimeMillisFn

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler(
  backend: Backend,
  delta: Long = 1000,
  tolerance: Long = 50,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis
) extends Scheduler {

  override def apply(): Unit = loop(currentTimeMillis())

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
      TimeUtil.sleep(diff, currentTimeMillis)
    }

    tick(t0, t1)

    loop(currentTimeMillis())
  }

  private def tick(t0: Long, t1: Long): Unit = {
    Await.result(backend.pullJobs(), Duration.Inf)
      .foreach { jobSpec =>
        jobSpec.triggers
          .flatMap(trigger => trigger.next(t0))
          .find(triggerTime => triggerTime < t1)
          .foreach(execute(_, jobSpec))
      }
  }

  private def execute(time: Long, jobSpec: JobSpec): Unit = {

  }

}