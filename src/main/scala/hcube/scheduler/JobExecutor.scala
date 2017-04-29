package hcube.scheduler

import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend
import hcube.scheduler.backend.Backend._
import hcube.scheduler.job.Job
import hcube.scheduler.model.{ExecState, ExecTrace, JobSpec}
import hcube.scheduler.utils.TimeUtil.TimeMillisFn

import scala.compat.Platform._
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class JobExecutor(
  backend: Backend,
  jobDispatch: (String) => Job,
  commitSuccess: Boolean = false,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis
)(
  implicit val ec: ExecutionContext
) extends TickReceiver {

  import JobExecutor._

  override def tick(t0: Long, t1: Long): Unit = {
    val jobs = Await.result(backend.pull(), Duration.Inf)
    logger.debug(s"Tick, t0: $t0, t1: $t1, jobs: ${jobs.size}")
    jobs.foreach { jobSpec =>
      jobSpec.triggers
        .flatMap(trigger => trigger.next(t0))
        .find(triggerTime => triggerTime < t1)
        .foreach(execute(_, jobSpec))
    }
  }

  private def execute(time: Long, jobSpec: JobSpec): Unit = {
    logger.debug(s"Triggering job execution, jobId: ${jobSpec.jobId}")
    val runningExecState = ExecState(TriggeredState, currentTimeMillis())
    val trace = ExecTrace(jobSpec.jobId, time, List(runningExecState))
    backend.transition(InitialState, TriggeredState, trace).foreach {
      case TransitionSuccess(_) =>
        Try(jobDispatch(jobSpec.typ)(time, jobSpec.payload)) match {
          case _: Success[_] =>
            logger.debug(s"Job execution succeeded, jobId: ${jobSpec.jobId}")
            if (commitSuccess) {
              val successExecState = ExecState(SuccessState, currentTimeMillis())
              backend.transition(TriggeredState, SuccessState,
                trace.copy(history = successExecState :: trace.history))
            }
          case Failure(e) =>
            logger.error(s"Job execution failed, jobId: ${jobSpec.jobId}", e)
            val msg = e.getMessage + EOL + e.getStackTrace.mkString("", EOL, EOL)
            val failureExecState = ExecState(FailureState, currentTimeMillis(), msg)
            backend.transition(TriggeredState, FailureState,
              trace.copy(history = failureExecState :: trace.history))
        }
      case TransitionFailed(_) => ()
    }
  }

}

object JobExecutor {

  val logger = Logger(getClass)

}
