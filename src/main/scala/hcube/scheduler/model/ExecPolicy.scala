package hcube.scheduler.model

import scala.concurrent.duration.Duration

case class FailurePolicy(maxFailures: Int = 3)

trait ExecPolicy {

  val failurePolicy: FailurePolicy

  def apply(): Unit

}

case class RetryExecPolicy(
  failurePolicy: FailurePolicy,
  limit: Int = 1,
  retryPeriod: Duration = Duration.Zero
) extends ExecPolicy {

  override def apply(): Unit = ???

}

case class CurrentExecPolicy(failurePolicy: FailurePolicy) extends ExecPolicy {

  override def apply(): Unit = ???

}