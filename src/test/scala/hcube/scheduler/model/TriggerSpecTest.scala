package hcube.scheduler.model

import java.text.SimpleDateFormat
import java.time.{Instant, Duration}

import org.specs2.mutable.Specification


class TriggerSpecTest extends Specification {

  private val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  private def ts(str: String) = df.parse(str).toInstant

  "time trigger" >> {
    val now = System.currentTimeMillis()
    val start: Long = now + 60000
    val interval: Long = 10000
    val trigger = TimeTriggerSpec(start, interval, repeat = 3)

    val next = trigger.next(now)
    val next1 = trigger.next(start + 5000)
    val next2 = trigger.next(start + 15000)
    val next3 = trigger.next(start + 25000)
    val next4 = trigger.next(start + 35000)

    Some(start) must_== next
    Some(start + interval) must_== next1
    Some(start + 2 * interval) must_== next2
    Some(start + 3 * interval) must_== next3
    None must_== next4
  }

  "cron trigger" >> {
    val now = ts("2017-01-06T11:17:07UTC").toEpochMilli
    val trigger = CronTriggerSpec("*/15 * * * *")
    val trigger2 = CronTriggerSpec("*/15 * * * * ?", cronType = "QUARTZ")

    val next = trigger.next(now)
    val next2 = trigger2.next(now)

    Some(ts("2017-01-06T11:30:00UTC").toEpochMilli) must_== next
    Some(ts("2017-01-06T11:17:15UTC").toEpochMilli) must_== next2
  }

}
