package hcube.scheduler.model

case class JobSpec(
  jobId: String,
  triggers: Seq[TriggerSpec],
  creationTime: Long,
  policy: Option[ExecPolicy],
  typ: String,
  name: Option[String],
  payload: Map[String, Any]
)
