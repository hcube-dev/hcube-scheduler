package hcube.scheduler.utils

import java.time.ZoneId

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object TimeUtil {

  val UTC = ZoneId.of("UTC")

  @tailrec def sleep(ms: Long): Unit = {
    val t0 = System.currentTimeMillis()
    Try(Thread.sleep(ms)) match {
      case _: Success[Unit] => ()
      case _: Failure[Unit] =>
        val diff = System.currentTimeMillis() - t0
        if (diff < ms) {
          sleep(ms - diff)
        }
    }
  }

}
