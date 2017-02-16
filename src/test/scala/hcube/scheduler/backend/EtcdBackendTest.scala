package hcube.scheduler.backend

import java.text.SimpleDateFormat

import com.coreos.jetcd.EtcdClientBuilder
import hcube.scheduler.model.ExecTrace
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class EtcdBackendTest extends Specification {

  args(skipAll = true)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  private def ts(str: String) = df.parse(str).toInstant

  val client = EtcdClientBuilder
    .newBuilder()
    .endpoints("http://hcube-etcd-v3-1-0-node-0:2379")
    .build()
  val jsonFormat = new JsonStorageFormat
  val backend = new EtcdBackend(client, jsonFormat)

  "pull jobs" >> {
    val future = backend.pullJobs()
    val jobs = Await.result(future, Duration.Inf)

    1 must_== 1
  }

  "update CAS" >> {
    val time = ts("2017-01-06T11:17:07UTC").toEpochMilli
    val trace = ExecTrace("qwer5", time, Nil)
    val future = backend.transition("running", "success", trace)
    val result = Await.result(future, Duration.Inf)

    1 must_== 1
  }

}
