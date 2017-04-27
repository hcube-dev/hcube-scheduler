package hcube.scheduler

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.Logger
import hcube.scheduler.utils.JavaUtil._
import hcube.scheduler.utils.TimeUtil.{SleepFn, TimeMillisFn}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, blocking}

trait Clock {

  def apply(): Future[Unit]

  def stop(): Unit

}

trait TickReceiver {

  def tick(t0: Long, t1: Long): Unit

}

class ScheduledClock(
  tickReceiver: TickReceiver,
  tickPeriod: Long = 1000
)(
  implicit ec: ExecutionContext
) extends Clock {

  private val scheduler = Executors.newScheduledThreadPool(1)

  override def apply(): Future[Unit] = {
    val now = System.currentTimeMillis()
    val initialDelay = (now / tickPeriod + 1) * tickPeriod - now

    val dispatchTickFn = () => {
      val now = System.currentTimeMillis()
      val t0 = (now / tickPeriod) * tickPeriod
      val t1 = t0 + tickPeriod
      tickReceiver.tick(t0, t1)
    }

    Future {
      blocking {
        val jFuture = scheduler.scheduleAtFixedRate(
          dispatchTickFn, initialDelay, tickPeriod, TimeUnit.MILLISECONDS)
        jFuture.get()
      }
    }
  }

  override def stop(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
  }

}

class LoopClock(
  tickReceiver: TickReceiver,
  tickPeriod: Long = 1000,
  tolerance: Long = 50,
  continueOnInterrupt: Boolean = false,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis,
  sleep: SleepFn = Thread.sleep,
  stopTime: Option[Long] = None
)(
  implicit ec: ExecutionContext
) extends Clock {

  import LoopClock._

  val stopFlag = new AtomicBoolean(false)

  override def stop(): Unit = {
    stopFlag.set(true)
  }

  override def apply(): Future[Unit] = Future { interruptHandler() }

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
  @tailrec private def loop(now: Long, prev: Long = Long.MinValue): Unit = {
    if (stopFlag.get()) {
      logger.info("Stopping")
      return
    }
    if (stopTime.isDefined && stopTime.forall(t => now >= t)) {
      // stop scheduler at given time, used in tests
      logger.info("Stop time reached")
      return
    }

    val (t0, t1) = computeInterval(now, prev, tolerance = tickPeriod / 2, maxPrevDiff = tickPeriod)

    val diff = t0 - now
    if (diff > tolerance) {
      // sleep until interval starts
      sleep(diff)
    }

    tickReceiver.tick(t0, t1)

    loop(currentTimeMillis(), prev = t1)
  }

  private def computeInterval(
    now: Long,
    prev: Long,
    tolerance: Long,
    maxPrevDiff: Long
  ): (Long, Long) = {
    val currentInterval = (now / tickPeriod) * tickPeriod
    val prevDiff = currentInterval - prev
    val nowDiff = now - currentInterval

    if (prevDiff == 0 || prev == Long.MinValue) {
      if (nowDiff <= tolerance) {
        (currentInterval, currentInterval + tickPeriod)
      } else {
        val nextInterval = currentInterval + tickPeriod
        (nextInterval, nextInterval + tickPeriod)
      }
    } else if (prevDiff > 0 && prevDiff <= maxPrevDiff) {
      if (nowDiff <= tolerance) {
        (prev, currentInterval + tickPeriod)
      } else {
        val nextInterval = currentInterval + tickPeriod
        (prev, nextInterval + tickPeriod)
      }
    } else {
      val nextInterval = currentInterval + tickPeriod
      (nextInterval, nextInterval + tickPeriod)
    }
  }

}

object LoopClock {

  val logger = Logger(getClass)

}
