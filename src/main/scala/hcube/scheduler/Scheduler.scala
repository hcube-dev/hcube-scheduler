package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.cleanup.CleanUpTask
import hcube.scheduler.tasks.TaskRunner

import scala.concurrent.ExecutionContext

trait Scheduler {

  val backend: Backend

  def apply(): Unit

  def shutdown(): Unit

}

class RootScheduler(
  override val backend: Backend,
  clock: Clock,
  cleanUpDisable: Boolean,
  cleanUpDelayMillis: Long,
  cleanUpJobsCount: Int
)(implicit ec: ExecutionContext) extends Scheduler {

  private val runner = new TaskRunner(1)

  override def apply(): Unit = {
    if (!cleanUpDisable) {
      runner.schedule(CleanUpTask(backend, cleanUpJobsCount), cleanUpDelayMillis)
    }
    clock()
    runner.shutdown()
  }

  override def shutdown(): Unit = clock.stop()

}
