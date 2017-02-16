package hcube.scheduler.utils

import java.time.ZoneId

import scala.annotation.tailrec

object TimeUtil {

  type TimeMillisFn = () => Long

  val UTC: ZoneId = ZoneId.of("UTC")

  @tailrec def sleep(ms: Long, currentTimeMillis: TimeMillisFn = System.currentTimeMillis): Unit = {
    val t0 = currentTimeMillis()
    try {
      Thread.sleep(ms)
    } catch {
      case _: InterruptedException =>
        val diff = currentTimeMillis() - t0
        if (diff < ms) {
          sleep(ms - diff, currentTimeMillis)
        }
    }
  }

}
