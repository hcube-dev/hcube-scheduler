package hcube.scheduler

import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend
import hcube.scheduler.backend.Backend._
import hcube.scheduler.job.Job
import hcube.scheduler.model.{ExecState, ExecTrace, JobSpec}
import hcube.scheduler.utils.TimeUtil
import hcube.scheduler.utils.TimeUtil.TimeMillisFn

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.compat.Platform._

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler(
  backend: Backend,
  delta: Long = 1000,
  tolerance: Long = 50,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis,
  jobDispatch: (String) => Job = Job.apply
)(
  implicit val ec: ExecutionContext
) extends Scheduler {

  import LoopScheduler._

  override def apply(): Unit = loop(currentTimeMillis())

  private def nextInterval(now: Long): Long = (now / delta) * delta + delta

  /**
    * [t0, t1] - time boundaries for current interval
    * t0 - beginning time of the current interval
    * t1 - end time of the current interval
    * t1 = t0 + delta
    */
  @tailrec private def loop(now: Long): Unit = {
    val t0 = nextInterval(now)
    val t1 = t0 + delta

    val diff = t0 - now
    if (diff > tolerance) {
      // sleep until interval starts
      TimeUtil.sleep(diff, currentTimeMillis)
    }

    tick(t0, t1)

    loop(currentTimeMillis())
  }

  private def tick(t0: Long, t1: Long): Unit = {
    Await.result(backend.pullJobs(), Duration.Inf)
      .foreach { jobSpec =>
        jobSpec.triggers
          .flatMap(trigger => trigger.next(t0))
          .find(triggerTime => triggerTime < t1)
          .foreach(execute(_, jobSpec))
      }
  }

  private def execute(time: Long, jobSpec: JobSpec): Unit = {
    logger.debug(s"Triggering job execution, jobId: ${jobSpec.jobId}")
    val runningExecState = ExecState(RunningState, currentTimeMillis())
    val trace = ExecTrace(jobSpec.jobId, time, List(runningExecState))
    backend.transition(InitialState, RunningState, trace).foreach {
      case TransitionSuccess(_) =>
        Try(jobDispatch(jobSpec.typ)(time, jobSpec.payload)) match {
          case _: Success[Unit] =>
            logger.debug(s"Job execution succeeded, jobId: ${jobSpec.jobId}")
            val successExecState = ExecState(SuccessState, currentTimeMillis())
            backend.transition(RunningState, SuccessState,
              trace.copy(history = successExecState :: trace.history))
          case Failure(e) =>
            logger.error(s"Job execution failed, jobId: ${jobSpec.jobId}", e)
            val msg = e.getMessage + EOL + e.getStackTrace.mkString("", EOL, EOL)
            val failureExecState = ExecState(FailureState, currentTimeMillis(), msg)
            backend.transition(RunningState, FailureState,
              trace.copy(history = failureExecState :: trace.history))
        }
      case TransitionFailed(_) => ()
    }
  }

}

object LoopScheduler {

  val logger = Logger(getClass)

}