package hcube.scheduler.model

import java.time.Instant

import scala.concurrent.duration.Duration

trait TriggerSpec {

  def next(): Instant

}

case class CronTriggerSpec(cron: Seq[String]) extends TriggerSpec {

  override def next(): Instant = ???

}

case class TimeTriggerSpec(
  start: Instant,
  interval: Duration = Duration.Zero,
  repeat: Int = 1
) extends TriggerSpec {

  override def next(): Instant = ???

}
