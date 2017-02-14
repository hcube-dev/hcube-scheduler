package hcube.scheduler

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler extends Scheduler {

  override def apply(): Unit = ???

}