package hcube.scheduler

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.TimeUtil.{SleepFn, TimeMillisFn}

import scala.annotation.tailrec

trait Clock {

  def apply(): Unit

}

trait Tickable {

  def tick(t0: Long, t1: Long): Unit

}

class LoopClock(
  tickable: Tickable,
  delta: Long = 1000,
  tolerance: Long = 50,
  continueOnInterrupt: Boolean = false,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis,
  sleep: SleepFn = Thread.sleep,
  stopTime: Option[Long] = None
) extends Clock {

  import LoopClock._

  override def apply(): Unit = interruptHandler()

  @tailrec private def interruptHandler(): Unit = {
    try {
      loop(currentTimeMillis())
    } catch {
      case _: InterruptedException =>
        if (continueOnInterrupt) {
          interruptHandler()
        }
    }
  }

  /**
    * [t0, t1] - time boundaries for current interval
    * t0 - beginning time of the current interval
    * t1 - end time of the current interval
    * t1 = t0 + delta
    */
  @tailrec private def loop(now: Long, prev: Long = 0L): Unit = {
    if (stopTime.isDefined && stopTime.forall(t => now >= t)) {
      // stop scheduler at given time, used in tests
      logger.info("Stop time reached")
      return
    }

    val t0 = nextInterval(now, prev)
    val t1 = t0 + delta

    val diff = t0 - now
    if (diff > tolerance) {
      // sleep until interval starts
      sleep(diff)
    }

    tickable.tick(t0, t1)

    loop(currentTimeMillis(), prev = t0)
  }

  private def nextInterval(now: Long, prev: Long): Long = {
    val currentInterval = (now / delta) * delta
    if ((now - currentInterval) <= tolerance && currentInterval != prev) {
      currentInterval
    } else {
      val t0 = currentInterval + delta
      if (t0 > prev) t0 else t0 + delta
    }
  }

}

object LoopClock {

  val logger = Logger(getClass)

}
