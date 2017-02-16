package hcube.scheduler.backend

import com.typesafe.scalalogging.Logger
import hcube.scheduler.model.JobSpec

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait JobSpecCache extends Backend {

  import JobSpecCache._

  val lifetime: Int

  @volatile private[backend] var cacheState = CacheState(Empty, None, Seq.empty, 0)

  abstract override def pullJobs(): Future[Seq[JobSpec]] = {
    val currentState = cacheState
    val state = updateCacheState(currentState, System.currentTimeMillis(), super.pullJobs())
      .map(newState => {
        updateInternalState(currentState, newState)
        newState
      }).getOrElse(currentState)
    Future.successful(state.cache)
  }

  private[backend] def updateCacheState(
    state: CacheState,
    now: Long,
    fn: => Future[Seq[JobSpec]]
  ): Option[CacheState] = {

    def update() = CacheState(Updating, Some(fn.map {
      jobSpecs => CacheState(Full, None, jobSpecs, now)
    }), state.cache, state.lastUpdate)

    state.state match {
      case Empty => Some(update())
      case Full =>
        if (isOutOfDate(state, now)) {
          Some(update())
        } else {
          None
        }
      case Updating => None
    }
  }

  private[backend] def isOutOfDate(cacheState: CacheState, currentTime: Long): Boolean = {
    cacheState.lastUpdate + lifetime < currentTime
  }

  private[backend] def updateInternalState(oldState: CacheState, newState: CacheState): Unit = {
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
  }

  private[backend] sealed trait State

  private[backend] case object Updating extends State

  private[backend] case object Full extends State

  private[backend] case object Empty extends State

  private[backend] case class CacheState(
    state: State,
    nextState: Option[Future[CacheState]],
    cache: Seq[JobSpec],
    lastUpdate: Long
  )

}

private object JobSpecCache {
  private val logger = Logger(getClass)
}