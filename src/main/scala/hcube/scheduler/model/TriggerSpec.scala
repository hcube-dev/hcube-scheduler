package hcube.scheduler.model

import java.time.{Instant, ZonedDateTime}

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import hcube.scheduler.utils.TimeUtil



trait TriggerSpec {

  def next(now: Long): Option[Long]

}

case class CronTriggerSpec(cron: String, cronType: String = "UNIX") extends TriggerSpec {

  private val parser = new CronParser(
    CronDefinitionBuilder.instanceDefinitionFor(CronType.valueOf(cronType)))

  private val execTime = ExecutionTime.forCron(parser.parse(cron))

  override def next(now: Long): Option[Long] = {
    val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now - 1), TimeUtil.UTC)
    if (execTime.isMatch(dateTime)) {
      Some(now)
    } else {
      Some(execTime.nextExecution(dateTime).toInstant.toEpochMilli)
    }
  }

}

case class TimeTriggerSpec(
  startMillis: Long,
  intervalMillis: Long = 60000,
  repeat: Int = 1
) extends TriggerSpec {

  override def next(now: Long): Option[Long] = {
    if (startMillis >= now) {
      Some(startMillis)
    } else {
      val diff = (now - startMillis) / intervalMillis
      if (diff < repeat) {
        Some(startMillis + (diff + 1) * intervalMillis)
      } else {
        None
      }
    }
  }

}
