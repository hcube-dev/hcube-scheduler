package hcube.scheduler.backend

import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.concurrent.Future
import scala.util.Try

trait Backend {

  case class UpdateResponse(success: Boolean, exec: ExecTrace)

  def pullJobs(): Future[Seq[JobSpec]]

  def updateExecTxn(exec: ExecTrace): Future[UpdateResponse]

}
