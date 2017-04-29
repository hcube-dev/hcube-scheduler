package hcube.scheduler.backend

import java.util.concurrent.TimeUnit

import com.coreos.jetcd.EtcdClientBuilder
import hcube.scheduler.model.{CronTriggerSpec, JobSpec}
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class EtcdBackendTest extends Specification {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val jsonFormat = new JsonStorageFormat

  val job1 = JobSpec(
    jobId = "job1",
    triggers = Seq(CronTriggerSpec("* * * * *")),
    creationTime = System.currentTimeMillis(),
    policy = None,
    typ = "log",
    name = None,
    payload = Map[String, Any]("foo" -> 1)
  )

  val job2 = JobSpec(
    jobId = "job2",
    triggers = Seq(CronTriggerSpec("* * * * *")),
    creationTime = System.currentTimeMillis(),
    policy = None,
    typ = "log",
    name = None,
    payload = Map[String, Any]("bar" -> 2)
  )

  "crud" >> {
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints("http://hcube-scheduler-etcd-0:2379")
      .build()
    val backend = new EtcdBackend(client, jsonFormat)

    val job1Key = Await.result(backend.put(job1), Duration(10, TimeUnit.SECONDS))
    val job2Key = Await.result(backend.put(job2), Duration(10, TimeUnit.SECONDS))

    val jobs = Await.result(backend.pull(), Duration(10, TimeUnit.SECONDS))

    jobs must contain(job1)
    jobs must contain(job2)

    Await.result(backend.delete(job1.jobId), Duration(15, TimeUnit.SECONDS))
    Await.result(backend.delete(job2.jobId), Duration(15, TimeUnit.SECONDS))

    val jobs2 = Await.result(backend.pull(), Duration(10, TimeUnit.SECONDS))

    jobs2 must not contain(job1)
    jobs2 must not contain(job2)
  }

}
