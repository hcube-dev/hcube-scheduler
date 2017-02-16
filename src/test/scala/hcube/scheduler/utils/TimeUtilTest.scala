package hcube.scheduler.utils

import org.specs2.mutable.Specification

class TimeUtilTest extends Specification {

  "sleep" >> {
    val t0 = System.currentTimeMillis()
    TimeUtil.sleep(1150L)
    val diff = System.currentTimeMillis() - t0
    diff must be_>=(1100L)
    diff must be_<=(1200L)
  }

}
