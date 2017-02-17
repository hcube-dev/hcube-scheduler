package hcube.scheduler.cleanup

import hcube.scheduler.backend.Backend

case class CleanUpTask(backend: Backend) extends (() => Unit) {

  override def apply() = {
    println("Clean up!")
  }

}
