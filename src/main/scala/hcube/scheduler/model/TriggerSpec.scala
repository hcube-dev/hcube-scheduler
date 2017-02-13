package hcube.scheduler.model

import java.time.Instant

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

trait TriggerSpec {

  def next(now: Instant): Option[Instant]

}

case class CronTriggerSpec(cron: Seq[String]) extends TriggerSpec {

  override def next(now: Instant): Option[Instant] = ???

}

case class TimeTriggerSpec(
  start: Instant,
  interval: Duration = 1 minute,
  repeat: Int = 1
) extends TriggerSpec {

  private val startMillis = start.toEpochMilli
  private val intervalMillis = interval.toMillis

  override def next(now: Instant): Option[Instant] = {
    if (start == now || start.isAfter(now)) {
      Some(start)
    } else {
      val diff = (now.toEpochMilli - startMillis) / intervalMillis
      if (diff < repeat) {
        Some(start.plusMillis((diff + 1) * intervalMillis))
      } else {
        None
      }
    }
  }

}
