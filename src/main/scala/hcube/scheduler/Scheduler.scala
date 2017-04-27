package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.tasks.{CleanUpTask, TaskRunner}

import scala.concurrent.{ExecutionContext, Future}

trait Scheduler {

  val backend: Backend

  def apply(): Future[Unit]

  def shutdown(): Unit

}

class DefaultScheduler(
  override val backend: Backend,
  clock: Clock,
  cleanUpDisable: Boolean,
  cleanUpDelayMillis: Long,
  cleanUpJobsCount: Int
)(
  implicit ec: ExecutionContext
) extends Scheduler {

  private val taskRunner = new TaskRunner(1)

  override def apply(): Future[Unit] = {
    if (!cleanUpDisable) {
      taskRunner.schedule(CleanUpTask(backend, cleanUpJobsCount), cleanUpDelayMillis)
    }
    clock().andThen {
      case _ => taskRunner.shutdown()
    }
  }

  override def shutdown(): Unit = clock.stop()

}
