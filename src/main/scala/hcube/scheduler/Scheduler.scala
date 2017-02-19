package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.cleanup.CleanUpTask
import hcube.scheduler.tasks.TaskRunner

import scala.concurrent.ExecutionContext

trait Scheduler {

  def apply(): Unit

}

class RootScheduler(
  backend: Backend,
  clock: Clock,
  cleanUpDisable: Boolean,
  cleanUpDelayMillis: Long,
  cleanUpJobsCount: Int
)(implicit ec: ExecutionContext) extends Scheduler {

  private val runner = new TaskRunner(1)

  override def apply() = {
    if (!cleanUpDisable) {
      runner.schedule(CleanUpTask(backend, cleanUpJobsCount), cleanUpDelayMillis)
    }
    clock()
    runner.shutdown()
  }

}
