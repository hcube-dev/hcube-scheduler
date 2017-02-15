package hcube.scheduler.backend

import java.time.Instant

import hcube.scheduler.model._
import org.specs2.mutable.Specification

class StorageFormatTest extends Specification {

  "json serialize/deserialize" >> {
    val now = Instant.now()
    val start = now.plusSeconds(60)
    val format = new JsonStorageFormat
    val jobSpec = JobSpec(
      jobId = "asdf1234",
      triggers = Seq(TimeTriggerSpec(start), CronTriggerSpec("* * * * *")),
      creation = now,
//      policy = RetryExecPolicy(RetryFailurePolicy()),
      typ = "http",
      name = Some("name"),
      payload = Map[String, Any]("foo" -> 1, "bar" -> 2)
    )

    val json = format.serialize(jobSpec)
    val jobSpec2 = format.deserialize[JobSpec](json)

    jobSpec must_== jobSpec2
  }

}
