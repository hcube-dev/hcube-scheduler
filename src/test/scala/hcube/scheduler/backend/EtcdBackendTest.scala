package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClientBuilder
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class EtcdBackendTest extends Specification {

  args(skipAll = true)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "pull jobs from etcd" >> {
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints("http://hcube-etcd-v3-1-0-node-0:2379")
      .build()
    val jsonFormat = new JsonStorageFormat
    val backend = new EtcdBackend(client, jsonFormat)

    val future = backend.pullJobs()
    val jobs = Await.result(future, Duration.Inf)

    1 must_== 1
  }

}
