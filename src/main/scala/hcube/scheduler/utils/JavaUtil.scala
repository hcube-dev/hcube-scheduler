package hcube.scheduler.utils

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

object JavaUtil {

  implicit class ListenableFutureExtras[T](lf: ListenableFuture[T]) {

    def toScalaFuture: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t
        def onSuccess(result: T): Unit = p success result
      })
      p.future
    }

  }

  implicit def functionToRunnable(f: () => Unit): Runnable =
    new Runnable() { def run(): Unit = f() }

}