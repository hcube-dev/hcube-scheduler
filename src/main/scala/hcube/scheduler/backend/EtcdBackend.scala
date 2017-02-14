package hcube.scheduler.backend

import hcube.scheduler.model.{ExecTrace, JobSpec}

import scala.util.Try

class EtcdBackend extends Backend {

  override def pullJobs(): Seq[JobSpec] = ???

  override def casUpdate(exec: ExecTrace): Try[ExecTrace] = ???

}
