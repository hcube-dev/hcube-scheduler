package hcube.scheduler

trait SchedEngine {

  def apply(): Unit

}

class LoopSchedEngine extends SchedEngine {

  override def apply(): Unit = ???

}