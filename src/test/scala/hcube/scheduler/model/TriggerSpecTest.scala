package hcube.scheduler.model

import java.time.Instant

import org.specs2.mutable.Specification

import scala.concurrent.duration._

class TriggerSpecTest extends Specification {

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

}
