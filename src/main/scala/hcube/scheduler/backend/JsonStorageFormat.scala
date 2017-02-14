package hcube.scheduler.backend

import java.time.{Duration, Instant}

import hcube.scheduler.model.{CronTriggerSpec, JobSpec, TimeTriggerSpec, TriggerSpec}
import org.json4s.JsonDSL._
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, JObject, MappingException, _}

class JsonStorageFormat extends StorageFormat {

  implicit val formats = org.json4s.DefaultFormats +
    InstantSerializer + DurationSerializer + TriggerSpecSerializer

  override def serialize(jobSpec: JobSpec): String = Serialization.write(jobSpec)

  override def deserialize(json: String): JobSpec =
    JsonMethods.parse(json, useBigDecimalForDouble = false).extract[JobSpec]

}

object TriggerSpecSerializer extends CustomSerializer[TriggerSpec](format => (
  {
    case obj: JObject =>
      implicit val formats = org.json4s.DefaultFormats +
        InstantSerializer + DurationSerializer
      val triggerType = (obj \ "triggerType").extract[String]
      triggerType match {
        case "cron" =>
          val cron = (obj \ "cron").extract[String]
          val cronType = (obj \ "cronType").extract[String]
          CronTriggerSpec(cron, cronType)
        case "time" =>
          val start = (obj \ "start").extract[Instant]
          val interval = (obj \ "interval").extract[Duration]
          val repeat = (obj \ "repeat").extract[Int]
          TimeTriggerSpec(start, interval, repeat)
        case x => throw new MappingException(s"Unknown trigger type: $triggerType")
      }
  },
  {
    case triggerSpec: TriggerSpec =>
      implicit val formats = org.json4s.DefaultFormats
      triggerSpec match {
        case CronTriggerSpec(cron, cronType) =>
          ("triggerType" -> "cron") ~
          ("cron" -> cron) ~
          ("cronType" -> cronType)
        case TimeTriggerSpec(start, interval, repeat) =>
          ("triggerType" -> "time") ~
          ("start" -> start.toEpochMilli) ~
          ("interval" -> interval.toMillis) ~
          ("repeat" -> repeat)
      }
  }
))

case object InstantSerializer extends CustomSerializer[Instant](format => (
  {
    case JInt(i) => Instant.ofEpochMilli(i.longValue)
    case JNull => null
  },
  {
    case i: Instant => JInt(i.toEpochMilli)
  }
))

case object DurationSerializer extends CustomSerializer[Duration](format => (
  {
    case JInt(i) => Duration.ofMillis(i.longValue)
    case JNull => null
  },
  {
    case d: Duration => JInt(d.toMillis)
  }
))
