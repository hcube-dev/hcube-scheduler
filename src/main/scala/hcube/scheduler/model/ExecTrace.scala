package hcube.scheduler.model

case class ExecTrace(
  jobId: String,
  time: Long,
  history: List[ExecState]
)

case class ExecState(state: String, time: Long, msg: String = "")
