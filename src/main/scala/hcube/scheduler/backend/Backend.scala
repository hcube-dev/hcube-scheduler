package hcube.scheduler.backend

import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.concurrent.Future
import scala.util.Try

trait Backend {

  case class UpdateResponse(success: Boolean, trace: ExecTrace)

  def pullJobs(): Future[Seq[JobSpec]]

  def transitionCAS(prevStatus: String, newStatus: String, trace: ExecTrace): Future[UpdateResponse]

}
