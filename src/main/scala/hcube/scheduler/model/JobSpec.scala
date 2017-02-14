package hcube.scheduler.model

import java.time.Instant

case class JobSpec(
  triggers: Seq[TriggerSpec],
  creation: Instant,
//  policy: ExecPolicy,
  typ: String,
  name: Option[String],
  payload: Map[String, Any]
)
