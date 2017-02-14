package hcube.scheduler.backend

import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.concurrent.Future
import scala.util.Try

trait Backend {

  def pullJobs(): Future[Seq[JobSpec]]

  def casUpdate(exec: ExecTrace): Try[ExecTrace]

}
