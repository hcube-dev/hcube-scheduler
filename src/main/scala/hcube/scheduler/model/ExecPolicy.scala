package hcube.scheduler.model


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
  retryPeriod: Long = 0
) extends ExecPolicy {

  override def apply(): Unit = ???

}

case class CurrentExecPolicy(failurePolicy: FailurePolicy) extends ExecPolicy {

  override def apply(): Unit = ???

}