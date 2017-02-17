package hcube.scheduler.backend

import hcube.scheduler.LoopScheduler
import hcube.scheduler.backend.Backend.TransitionSuccess
import hcube.scheduler.job.Job
import hcube.scheduler.model._
import org.mockito.{ArgumentCaptor, Matchers, Mockito => MockitoMockito}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class LoopSchedulerTest extends Specification with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val backend = mock[Backend]
  private val job = mock[Job]
  private val execTrace = mock[ExecTrace]

  private val timeQueue = mutable.Queue(1000L, 1500L, 3020L, 4100L)

  private val timeFn = () => {
    timeQueue.dequeue()
  }

  private val timeQueueJob = mutable.Queue(
    1001L, 1002L, 1003L, 2001L, 2002L, 2003L, 3001L, 3002L, 3003L)

  private val timeFnJob = () => {
    timeQueueJob.dequeue()
  }

  private val jobDispatch = (typ: String) => {
    job
  }

  private val jobSpecs = List(
    JobSpec(
      jobId = "job1",
      triggers = Seq(CronTriggerSpec("* * * * * ?", "QUARTZ")),
      creationTime = 0,
      policy = RetryExecPolicy(RetryFailurePolicy()),
      typ = "system",
      name = Some("job1"),
      payload = Map[String, Any]("cmd" -> "uname -a")
    )
  )

  "scheduler" >> {
    backend.pullJobs() returns Future.successful(jobSpecs)
    backend.transition(Matchers.anyString(), Matchers.anyString(), Matchers.any(classOf[ExecTrace]))
      .returns(Future.successful(TransitionSuccess(execTrace)))

    val scheduler = new LoopScheduler(
      backend,
      jobDispatch,
      delta = 1000,
      tolerance = 50,
      commitSuccess = false,
      continueOnInterrupt = false,
      currentTimeMillis = timeFn,
      currentTimeMillisJob = timeFnJob,
      sleep = (_: Long) => (),
      stopTime = Some(4000)
    )

    scheduler()

    val timeArg = ArgumentCaptor.forClass(classOf[Long])
    val payloadArg = ArgumentCaptor.forClass(classOf[Map[String, Any]])
    MockitoMockito.verify(job, MockitoMockito.times(3))
      .apply(timeArg.capture(), payloadArg.capture())

    List(1000L, 2000L, 3000L) should_== timeArg.getAllValues.toList
  }

}
