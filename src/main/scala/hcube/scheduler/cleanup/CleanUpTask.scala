package hcube.scheduler.cleanup

import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend
import hcube.scheduler.model.JobSpec

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class CleanUpTask(
  backend: Backend, numberOfJobsToRemove: Int
)(implicit ex: ExecutionContext) extends (() => Unit) {

  import CleanUpTask._

  def cleanUpJobsForJobSpec(jobSpec: JobSpec): Unit = {
    backend.removeOldJobs(jobSpec.jobId, numberOfJobsToRemove).onComplete {
      case Success(deleted) =>
        if (deleted > 0) {
          logger.debug(s"Cleaned '$deleted' jobs for JobSpec: '${jobSpec.jobId}'.")
        } else {
          logger.debug(s"No jobs to clean for JobSpec: ${jobSpec.jobId}.")
        }
      case Failure(exc) =>
        logger.error(s"Cannot clean jobs for ${jobSpec.jobId}.", exc)
    }
  }

  override def apply() = {
    backend.pullJobs().foreach(jobSpec => jobSpec.foreach(cleanUpJobsForJobSpec))
  }

}

object CleanUpTask {
  private val logger = Logger(getClass)
}