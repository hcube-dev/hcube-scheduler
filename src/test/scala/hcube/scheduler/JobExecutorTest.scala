package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.job.Job
import org.mockito.Matchers
import org.specs2.mock.{Mockito => MockitoSpec2}
import org.specs2.mutable.Specification

import scala.concurrent.{ExecutionContext, Future}

class JobExecutorTest extends Specification with MockitoSpec2 {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "simple tick" >> {
    val backend: Backend = mock[Backend].pullJobs() returns Future.successful(Seq())
    val job = mock[Job]
    val jobDispatch = mock[(String) => Job].apply(Matchers.anyString()) returns job

    val jobExecutor = new JobExecutor(backend, jobDispatch)
    jobExecutor.tick(0, 1)

    there was one(backend).pullJobs()
  }

}
