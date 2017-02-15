package hcube.scheduler.model

import java.time.Duration


trait FailurePolicy {

  def apply(): Unit

}

case class RetryFailurePolicy(maxFailures: Int = 3) extends FailurePolicy {

  override def apply(): Unit = ???

}

trait ExecPolicy {

  val failurePolicy: FailurePolicy

  def apply(): Unit

}

case class RetryExecPolicy(
  failurePolicy: FailurePolicy,
  limit: Int = 1,
  retryPeriod: Duration = Duration.ZERO
) extends ExecPolicy {

  override def apply(): Unit = ???

}

case class CurrentExecPolicy(failurePolicy: FailurePolicy) extends ExecPolicy {

  override def apply(): Unit = ???

}