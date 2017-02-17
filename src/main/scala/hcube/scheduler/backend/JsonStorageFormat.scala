package hcube.scheduler.backend

import java.time.{Duration, Instant}

import hcube.scheduler.model._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.Serialization.{read, write}
import org.json4s.{CustomSerializer, JObject, MappingException, _}

class JsonStorageFormat extends StorageFormat {

  implicit val formats = DefaultFormats + InstantSerializer + DurationSerializer +
    TriggerSpecSerializer + ExecPolicySerializer + FailurePolicySerializer

  override def serialize(obj: AnyRef): String = write(obj)

  override def deserialize[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T =
    JsonMethods.parse(json, useBigDecimalForDouble = false).extract[T]

}

object TriggerSpecSerializer extends CustomSerializer[TriggerSpec](format => (
  {
    case obj: JObject =>
      implicit val formats = DefaultFormats + InstantSerializer + DurationSerializer
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
      implicit val formats = DefaultFormats
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

object FailurePolicySerializer extends CustomSerializer[FailurePolicy](format => (
  {
    case obj: JObject =>
      implicit val formats = DefaultFormats
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

object ExecPolicySerializer extends CustomSerializer[ExecPolicy](format => (
  {
    case obj: JObject =>
      implicit val formats = DefaultFormats + FailurePolicySerializer + DurationSerializer
      val policyType = (obj \ "execPolicyType").extract[String]
      policyType match {
        case "current" =>
          val failurePolicy = (obj \ "failurePolicy").extract[FailurePolicy]
          CurrentExecPolicy(failurePolicy)
        case "retry" =>
          val failurePolicy = read[FailurePolicy]((obj \ "failurePolicy").extract[String])
          val limit = (obj \ "limit").extract[Int]
          val retryPeriod = (obj \ "retryPeriod").extract[Long]
          RetryExecPolicy(failurePolicy, limit, retryPeriod)
      }
  },
  {
    case policy: ExecPolicy =>
      implicit val formats = DefaultFormats + FailurePolicySerializer + DurationSerializer
      policy match {
        case CurrentExecPolicy(failurePolicy) =>
          ("execPolicyType" -> "current") ~
          ("failurePolicy" -> write(failurePolicy))
        case RetryExecPolicy(failurePolicy, limit, retryPeriod) =>
          ("execPolicyType" -> "retry") ~
          ("failurePolicy" -> write(failurePolicy)) ~
          ("limit" -> limit) ~
          ("retryPeriod" -> retryPeriod)
      }
  }
))

object InstantSerializer extends CustomSerializer[Instant](format => (
  {
    case JInt(i) => Instant.ofEpochMilli(i.longValue)
    case JNull => null
  },
  {
    case i: Instant => JInt(i.toEpochMilli)
  }
))

object DurationSerializer extends CustomSerializer[Duration](format => (
  {
    case JInt(i) => Duration.ofMillis(i.longValue)
    case JNull => null
  },
  {
    case d: Duration => JInt(d.toMillis)
  }
))
