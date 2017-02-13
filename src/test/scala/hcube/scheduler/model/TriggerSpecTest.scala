package hcube.scheduler.model

import java.text.SimpleDateFormat
import java.time.Instant

import org.specs2.mutable.Specification

import scala.concurrent.duration._

class TriggerSpecTest extends Specification {

  private val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  private def ts(str: String) = df.parse(str).toInstant

  "time trigger" >> {
    val now = Instant.now()
    val start = now.plusSeconds(60)
    val interval = 10 seconds
    val trigger = TimeTriggerSpec(start, interval, repeat = 3)

    val next = trigger.next(now)
    val next1 = trigger.next(start.plusSeconds(5))
    val next2 = trigger.next(start.plusSeconds(15))
    val next3 = trigger.next(start.plusSeconds(25))
    val next4 = trigger.next(start.plusSeconds(35))

    Some(start) must_== next
    Some(start.plusSeconds(interval.toSeconds)) must_== next1
    Some(start.plusSeconds(2 * interval.toSeconds)) must_== next2
    Some(start.plusSeconds(3 * interval.toSeconds)) must_== next3
    None must_== next4
  }

  "cron trigger" >> {
    val now = ts("2017-01-06T11:17:07UTC")
    val trigger = CronTriggerSpec("*/15 * * * *")
    val trigger2 = CronTriggerSpec("*/15 * * * * ?", cronType = "QUARTZ")

    val next = trigger.next(now)
    val next2 = trigger2.next(now)

    Some(ts("2017-01-06T11:30:00UTC")) must_== next
    Some(ts("2017-01-06T11:17:15UTC")) must_== next2
  }

}
