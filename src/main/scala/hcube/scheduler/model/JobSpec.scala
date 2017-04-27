package hcube.scheduler.model

import com.typesafe.config.Config
import scala.collection.JavaConversions._

case class JobSpec(
  jobId: String,
  triggers: Seq[TriggerSpec],
  creationTime: Long,
  policy: Option[ExecPolicy],
  typ: String,
  name: Option[String],
  payload: Map[String, Any]
)

object JobSpec {

  def fromConfig(config: Config): JobSpec = {
    val jobId = config.getString("jobId")
    val triggers = config.getConfigList("triggers").map(TriggerSpec.fromConfig)
    val creationTime = System.currentTimeMillis()
    val policy = None
    val typ = config.getString("typ")
    val payload = config.getConfig("payload").entrySet
      .map(entry => entry.getKey -> entry.getValue.unwrapped).toMap
    val name = None
    JobSpec(jobId, triggers, creationTime, policy, typ, name, payload)
  }

}
