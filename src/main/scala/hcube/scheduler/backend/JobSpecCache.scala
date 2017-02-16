package hcube.scheduler.backend

import com.typesafe.scalalogging.Logger
import hcube.scheduler.model.JobSpec

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait JobSpecCache extends Backend {

  import JobSpecCache._

  val lifetimeMillis: Int

  @volatile private[backend] var cacheState = CacheState(Empty, None, Nil, 0)

  abstract override def pullJobs(): Future[Seq[JobSpec]] = {
    val currentState = cacheState
    val state = updateCacheState(currentState, System.currentTimeMillis(), super.pullJobs())
      .map(newState => updateInternalState(currentState, newState))
      .getOrElse(currentState)
    Future.successful(state.cache)
  }

  private[backend] def updateCacheState(
    state: CacheState,
    now: Long,
    fn: => Future[Seq[JobSpec]]
  ): Option[CacheState] = {

    def update() = CacheState(Updating, Some(fn.map {
      jobSpecs => CacheState(Cached, None, jobSpecs, now)
    }), state.cache, state.lastUpdate)

    state.state match {
      case Empty => Some(update())
      case Cached =>
        if (isOutOfDate(state, now)) {
          Some(update())
        } else {
          None
        }
      case Updating => None
    }
  }

  private[backend] def isOutOfDate(cacheState: CacheState, currentTime: Long): Boolean = {
    cacheState.lastUpdate + lifetimeMillis < currentTime
  }

  private[backend] def updateInternalState(
    oldState: CacheState,
    newState: CacheState
  ): CacheState = {
    cacheState = newState
    // Updates the state to a next one if there is a pending next state request.
    newState.nextState.foreach(future => {
      future.onComplete {
        case Success(nextState) =>
          cacheState = nextState
        case Failure(exc) =>
          logger.error("Cannot update JobSpec cache.", exc)
          // In case of failure the old cache should be restored.
          cacheState = oldState
      }
    })
    cacheState
  }

}

private[backend] object JobSpecCache {

  private val logger = Logger(getClass)

  private[backend] sealed trait State

  private[backend] case object Updating extends State

  private[backend] case object Cached extends State

  private[backend] case object Empty extends State

  private[backend] case class CacheState(
    state: State,
    nextState: Option[Future[CacheState]],
    cache: Seq[JobSpec],
    lastUpdate: Long
  )

}