package hcube.scheduler

import hcube.scheduler.backend.Backend
import hcube.scheduler.backend.Backend._
import hcube.scheduler.job.Job
import hcube.scheduler.model.{ExecTrace, JobSpec, TriggerSpec}
import org.mockito.{Matchers => M}
import org.specs2.mock.{Mockito => MockitoSpecs2}
import org.specs2.mutable.Specification

import scala.concurrent.{ExecutionContext, Future}

class JobExecutorTest extends Specification with MockitoSpecs2 {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "execute job" >> {
    val trigger: TriggerSpec = mock[TriggerSpec].next(M.anyLong()) returns Some(1L)
    val jobSpec: JobSpec = mock[JobSpec].triggers returns Seq(trigger)
    val backend = mock[Backend]
    backend.pullJobs() returns Future.successful(Seq(jobSpec))
    backend.transition(M.eq(InitialState), M.eq(TriggeredState), M.any[ExecTrace])
      .returns(Future.successful(TransitionSuccess(mock[ExecTrace])))
    val job = mock[Job]
    val jobDispatch: (String) => Job = mock[(String) => Job].apply(M.anyString()) returns job
    val jobExecutor = new JobExecutor(backend, jobDispatch)

    jobExecutor.tick(0, 2)

    there was one(backend).pullJobs()
    there was one(backend).transition(M.eq(InitialState), M.eq(TriggeredState), M.any[ExecTrace])
    there was one(trigger).next(M.anyLong())
    there was one(job).apply(M.eq(1L), M.any())
  }


}
