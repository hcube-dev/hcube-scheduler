package hcube.scheduler.utils

import java.time.ZoneId

import scala.annotation.tailrec

object TimeUtil {

  type TimeMillisFn = () => Long
  type SleepFn = (Long) => Unit

  val UTC: ZoneId = ZoneId.of("UTC")

}
