package hcube.scheduler.backend

import hcube.scheduler.backend.Backend._
import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.concurrent.{ExecutionContext, Future}

trait Backend {

  implicit val ec: ExecutionContext

  def pull(): Future[Seq[JobSpec]]

  def transition(prevState: String, newState: String, trace: ExecTrace): Future[TransitionResult]

  def cleanup(jobId: String, numberOfJobsToPreserve: Int): Future[Long]

  def put(job: JobSpec): Future[String]

  def delete(jobId: String): Future[String]

}

object Backend {

  sealed trait TransitionResult

  case class TransitionSuccess(trace: ExecTrace) extends TransitionResult

  case class TransitionFailed(trace: ExecTrace) extends TransitionResult

  val InitialState = ""
  val TriggeredState = "triggered"
  val SuccessState = "success"
  val FailureState = "failure"

}
