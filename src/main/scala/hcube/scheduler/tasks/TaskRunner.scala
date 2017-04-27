package hcube.scheduler.tasks

import java.util.concurrent.{Executors, TimeUnit}

import hcube.scheduler.utils.JavaUtil._

class TaskRunner(numberOfTasks: Int) {

  private val scheduler = Executors.newScheduledThreadPool(numberOfTasks)

  def schedule(command: () => Unit, delayMillis: Long): Unit = {

    @volatile var lastExecution = 0L
    def commandFn = () => {
      val now = System.currentTimeMillis()
      if (now - lastExecution >= delayMillis / 2) {
        command()
        lastExecution = now
      }
    }

    scheduler.scheduleAtFixedRate(
      commandFn, delayMillis, delayMillis, TimeUnit.MILLISECONDS)
  }

  def shutdown(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
  }
}
