package hcube.scheduler

import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend
import hcube.scheduler.backend.Backend._
import hcube.scheduler.job.Job
import hcube.scheduler.model.{ExecState, ExecTrace, JobSpec}
import hcube.scheduler.utils.TimeUtil.TimeMillisFn

import scala.annotation.tailrec
import scala.compat.Platform._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

trait Scheduler {

  def apply(): Unit

}

class LoopScheduler(
  backend: Backend,
  jobDispatch: (String) => Job,
  delta: Long = 1000,
  tolerance: Long = 50,
  commitSuccess: Boolean = false,
  continueOnInterrupt: Boolean = false,
  currentTimeMillis: TimeMillisFn = System.currentTimeMillis,
  stopTime: Option[Long] = None
)(
  implicit val ec: ExecutionContext
) extends Scheduler {

  import LoopScheduler._

  override def apply(): Unit = interruptHandler()

  @tailrec private def interruptHandler(): Unit = {
    try {
      loop(currentTimeMillis())
    } catch {
      case _: InterruptedException =>
        if (continueOnInterrupt) {
          interruptHandler()
        }
    }
  }

  private def nextInterval(now: Long): Long = (now / delta) * delta + delta

  /**
    * [t0, t1] - time boundaries for current interval
    * t0 - beginning time of the current interval
    * t1 - end time of the current interval
    * t1 = t0 + delta
    */
  @tailrec private def loop(now: Long): Unit = {
    if (stopTime.isDefined && stopTime.forall(t => now >= t)) {
      // stop scheduler at given time, used in tests
      logger.info("Stop time reached")
      return
    }

    val t0 = nextInterval(now)
    val t1 = t0 + delta

    val diff = t0 - now
    if (diff > tolerance) {
      // sleep until interval starts
      Thread.sleep(diff)
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
          case _: Success[_] =>
            logger.debug(s"Job execution succeeded, jobId: ${jobSpec.jobId}")
            if (commitSuccess) {
              val successExecState = ExecState(SuccessState, currentTimeMillis())
              backend.transition(RunningState, SuccessState,
                trace.copy(history = successExecState :: trace.history))
            }
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