package hcube.scheduler.backend

import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.util.Try

trait SchedBackend {

  def pullJobs(): Seq[JobSpec]

  def casUpdate(exec: ExecTrace): Try[ExecTrace]

}
