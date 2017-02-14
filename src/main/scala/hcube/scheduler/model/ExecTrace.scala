package hcube.scheduler.model

import java.time.Instant

case class ExecTrace(
  jobId: String,
  time: Instant,
  history: Seq[ExecStatus]
)

case class ExecStatus(status: String, time: Instant)
