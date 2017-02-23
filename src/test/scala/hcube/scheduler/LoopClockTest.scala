package hcube.scheduler

import hcube.scheduler.utils.TimeUtil.SleepFn
import org.mockito.{ArgumentCaptor, Mockito => MockitoMockito}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext

class LoopClockTest extends Specification with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val tickable = mock[Tickable]

  private val timeQueue = mutable.Queue(1000L, 1300L, 2040L, 3020L, 4200L, 7200L, 8000L)

  private val timeFn = () => {
    timeQueue.dequeue()
  }

  private val sleep = mock[SleepFn]

  "generate expected tick intervals" >> {
    val clock = new LoopClock(
      tickable,
      tickTime = 1000,
      tolerance = 50,
      continueOnInterrupt = false,
      currentTimeMillis = timeFn,
      sleep = sleep,
      stopTime = Some(8000)
    )

    clock()

    val t0Arg = ArgumentCaptor.forClass(classOf[Long])
    val t1Arg = ArgumentCaptor.forClass(classOf[Long])
    MockitoMockito.verify(tickable, MockitoMockito.times(6))
      .tick(t0Arg.capture(), t1Arg.capture())

    val sleepArg = ArgumentCaptor.forClass(classOf[Long])
    MockitoMockito.verify(sleep, MockitoMockito.times(4))
      .apply(sleepArg.capture())

    List(1000L, 2000L, 3000L, 4000L, 5000L, 6000L) must_== t0Arg.getAllValues.toList
    List(2000L, 3000L, 4000L, 5000L, 6000L, 8000L) must_== t1Arg.getAllValues.toList

    List(700, 960, 980, 800L) must_== sleepArg.getAllValues.toList
  }

}
