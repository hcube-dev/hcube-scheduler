package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClientBuilder
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class JobSpecCacheTest extends Specification {

  implicit val exec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "test" >> {
    val client = EtcdClientBuilder
      .newBuilder()
      .endpoints("http://hcube-etcd-v3-1-0-node-0:2379")
      .build()
    val jsonFormat = new JsonStorageFormat
    val backend = new EtcdBackend(client, jsonFormat) with JobSpecCache {
      val lifetimeMillis = 5
    }
    backend.pullJobs()
    1 must_== 1
  }

}
