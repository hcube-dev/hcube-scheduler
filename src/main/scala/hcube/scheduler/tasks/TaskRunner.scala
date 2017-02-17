package hcube.scheduler.tasks

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class TaskRunner(numberOfTasks: Int) {

  private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(numberOfTasks)

  def schedule(command: () => Unit, delayMillis: Long): Unit = {
    scheduler.scheduleAtFixedRate(new Runnable {

      @volatile var lastExecution = 0L

      override def run() = {
        val now = System.currentTimeMillis()
        if (now - lastExecution >= delayMillis / 2) {
          command()
          lastExecution = now
        }
      }
    }, delayMillis, delayMillis, TimeUnit.MILLISECONDS)
  }

  def shutdown(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
  }
}
