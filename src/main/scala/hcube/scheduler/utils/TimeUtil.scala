package hcube.scheduler.utils

import java.time.ZoneId

import scala.annotation.tailrec

object TimeUtil {

  val UTC: ZoneId = ZoneId.of("UTC")

  @tailrec def sleep(ms: Long): Unit = {
    val t0 = System.currentTimeMillis()
    try {
      Thread.sleep(ms)
    } catch {
      case _: InterruptedException =>
        val diff = System.currentTimeMillis() - t0
        if (diff < ms) {
          sleep(ms - diff)
        }
    }
  }

}
