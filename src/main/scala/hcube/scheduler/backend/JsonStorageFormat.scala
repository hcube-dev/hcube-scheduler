package hcube.scheduler.backend

import hcube.scheduler.model._
import org.json4s.JsonDSL._
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, Extraction, JObject, MappingException, NoTypeHints}

class JsonStorageFormat extends StorageFormat {

  implicit val formats = Serialization.formats(NoTypeHints) +
    TriggerSpecSerializer + ExecPolicySerializer + FailurePolicySerializer

  override def serialize(obj: AnyRef): String = Serialization.write(obj)

  override def deserialize[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T =
    JsonMethods.parse(json, useBigDecimalForDouble = false).extract[T]

}

object TriggerSpecSerializer extends CustomSerializer[TriggerSpec](implicit format => (
  {
    case obj: JObject =>
      val triggerType = (obj \ "triggerType").extract[String]
      triggerType match {
        case "cron" =>
          val cron = (obj \ "cron").extract[String]
          val cronType = (obj \ "cronType").extract[String]
          CronTriggerSpec(cron, cronType)
        case "time" =>
          val start = (obj \ "start").extract[Long]
          val interval = (obj \ "interval").extract[Long]
          val repeat = (obj \ "repeat").extract[Int]
          TimeTriggerSpec(start, interval, repeat)
        case x => throw new MappingException(s"Unknown trigger type: $triggerType")
      }
  },
  {
    case triggerSpec: TriggerSpec =>
      triggerSpec match {
        case CronTriggerSpec(cron, cronType) =>
          ("triggerType" -> "cron") ~
          ("cron" -> cron) ~
          ("cronType" -> cronType)
        case TimeTriggerSpec(start, interval, repeat) =>
          ("triggerType" -> "time") ~
          ("start" -> start) ~
          ("interval" -> interval) ~
          ("repeat" -> repeat)
      }
  }
))

object FailurePolicySerializer extends CustomSerializer[FailurePolicy](implicit format => (
  {
    case obj: JObject =>
      val policyType = (obj \ "failurePolicyType").extract[String]
      policyType match {
        case "retry" =>
          val maxFailures = (obj \ "maxFailures").extract[Int]
          RetryFailurePolicy(maxFailures)
      }
  },
  {
    case policy: FailurePolicy =>
      policy match {
        case RetryFailurePolicy(maxFailures) =>
          ("failurePolicyType" -> "retry") ~
          ("maxFailures" -> maxFailures)
      }
  }
))

object ExecPolicySerializer extends CustomSerializer[ExecPolicy](implicit format => (
  {
    case obj: JObject =>
      val policyType = (obj \ "execPolicyType").extract[String]
      policyType match {
        case "current" =>
          val failurePolicy = (obj \ "failurePolicy").extract[FailurePolicy]
          CurrentExecPolicy(failurePolicy)
        case "retry" =>
          val failurePolicy = (obj \ "failurePolicy").extract[FailurePolicy]
          val limit = (obj \ "limit").extract[Int]
          val retryPeriod = (obj \ "retryPeriod").extract[Long]
          RetryExecPolicy(failurePolicy, limit, retryPeriod)
      }
  },
  {
    case policy: ExecPolicy =>
      policy match {
        case CurrentExecPolicy(failurePolicy) =>
          ("execPolicyType" -> "current") ~
          ("failurePolicy" -> Extraction.decompose(failurePolicy))
        case RetryExecPolicy(failurePolicy, limit, retryPeriod) =>
          ("execPolicyType" -> "retry") ~
          ("failurePolicy" -> Extraction.decompose(failurePolicy)) ~
          ("limit" -> limit) ~
          ("retryPeriod" -> retryPeriod)
      }
  }
))
