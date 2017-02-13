package hcube.scheduler.model

import java.time.{Instant, ZoneId, ZonedDateTime}

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser

import scala.concurrent.duration.Duration

trait TriggerSpec {

  def next(now: Instant): Option[Instant]

}

case class CronTriggerSpec(cron: String, cronType: String = "UNIX") extends TriggerSpec {

  private val utcZone = ZoneId.of("UTC")

  private val parser = new CronParser(
    CronDefinitionBuilder.instanceDefinitionFor(CronType.valueOf(cronType)))

  private val execTime = ExecutionTime.forCron(parser.parse(cron))

  override def next(now: Instant): Option[Instant] =
    Some(execTime.nextExecution(ZonedDateTime.ofInstant(now, utcZone)).toInstant)

}

case class TimeTriggerSpec(
  start: Instant,
  interval: Duration = Duration.Zero,
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
