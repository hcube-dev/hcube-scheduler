package hcube.scheduler.backend

import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.scala.AwaitilitySupport
import hcube.scheduler.backend.JobSpecCache._
import hcube.scheduler.model.{ExecTrace, JobSpec}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}

class JobSpecCacheTest extends Specification with Mockito with AwaitilitySupport {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "should update internal state" >> {

    var queue = Queue(jobSpec, jobSpecSecond)

    val backend = new TestBackend(Future {
      Thread.sleep(200)
      val (element, newQueue) = queue.dequeue
      queue = newQueue
      Seq(element)
    }) with JobSpecCache {
      val lifetimeMillis = 1000
    }

    Awaitility.await until {
      Await.result(backend.pull(), 10 seconds).isEmpty
    }

    // Wait for the fist update.
    Awaitility.await until {
      Await.result(backend.pull(), 10 seconds).equals(Seq(jobSpec))
    }

    // Wait for cache expiration and the next update.
    Awaitility.await until {
      Await.result(backend.pull(), 10 seconds).equals(Seq(jobSpecSecond))
    }

    1 must_== 1
  }

  "should return true if cache is out of date" >> {
    cachedBackend.isOutOfDate(CacheState(Cached, None, Seq(), 900), 1000) must_== false
  }

  "should return false if cache is up to date" >> {
    cachedBackend.isOutOfDate(CacheState(Cached, None, Seq(), 901), 1000) must_== false
  }

  "don't update state" >> {

    "if cache is up to date" >> {
      val stateOpt = cachedBackend.updateCacheState(CacheState(Cached, None, Seq(), 900), 1000,
        getJobs)
      stateOpt must_== None
    }

  }

  "update state" >> {

    "if cache is being updated" >> {
      val stateUpdating = CacheState(Updating, None, Seq(jobSpec), 20)
      val stateOpt = cachedBackend.updateCacheState(stateUpdating, 1000, getJobs)
      stateOpt must_== None
    }

    "if cache is out of date" >> {
      val stateOpt = cachedBackend.updateCacheState(CacheState(Empty, None, Seq(), 901), 1000,
        getJobs)

      stateOpt.isDefined must_== true
      val state = stateOpt.get

      state.state must_== Updating
      state.cache must_== Seq()
      state.lastUpdate must_== 901
      state.nextState must_!== None
      state.nextState.get.isCompleted must_== true
      Await.result(state.nextState.get, Duration.Inf) must_==
        CacheState(Cached, None, Seq(jobSpec), 1000)
    }

    "if from empty to cached" >> {

      val stateOpt = cachedBackend.updateCacheState(CacheState(Empty, None, Nil, 0), 1000, getJobs)

      stateOpt.isDefined must_== true
      val state = stateOpt.get

      state.state must_== Updating
      state.cache must_== Nil
      state.lastUpdate must_== 0
      state.nextState must_!== None
      state.nextState.get.isCompleted must_== true
      Await.result(state.nextState.get, Duration.Inf) must_==
        CacheState(Cached, None, Seq(jobSpec), 1000)
    }
  }

  val jobSpec = mock[JobSpec]
  val jobSpecSecond = mock[JobSpec]

  val Lifetime = 100

  def getJobs = Future.successful(Seq(jobSpec))

  val cachedBackend = new TestBackend(getJobs) with JobSpecCache {
    override val lifetimeMillis = Lifetime
  }

  class TestBackend(fn: => Future[Seq[JobSpec]]) extends Backend {

    override implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    override def pull() = fn

    override def transition(prevStatus: String, newStatus: String, trace: ExecTrace) = ???

    override def cleanup(jobId: String, numberOfJobsToPreserve: Int) = ???

    override def put(job: JobSpec): Future[String] = ???

    override def delete(jobId: String): Future[String] = ???

  }

}

