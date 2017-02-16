package hcube.scheduler.model

case class ExecTrace(
  jobId: String,
  time: Long,
  history: Seq[ExecState]
)

case class ExecState(state: String, time: Long, msg: String = "")
