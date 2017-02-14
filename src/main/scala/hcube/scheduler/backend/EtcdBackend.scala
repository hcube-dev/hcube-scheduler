package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClient
import com.coreos.jetcd.options.GetOption
import com.google.protobuf.ByteString
import hcube.scheduler.model.{ExecTrace, JobSpec}
import hcube.scheduler.utils.ListenableFutureUtil._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class EtcdBackend(
  etcd: EtcdClient,
  format: StorageFormat,
  dir: String = "/hcube/scheduler",
  charset: String = "ISO-8859-1"
)(
  implicit ec: ExecutionContext
) extends Backend {

  private val kvClient = etcd.getKVClient

  private val jobsKey = ByteString.copyFrom(dir + "/job/", charset)
  private val endKey = ByteString.copyFrom("\0", charset)
  private val getRangeOption = GetOption.newBuilder().withRange(endKey).build()

  override def pullJobs(): Future[Seq[JobSpec]] = {
    kvClient
      .get(jobsKey, getRangeOption)
      .asScala
      .map { response =>
        response.getKvsList.toList.map { kv =>
          format.deserialize(kv.getValue.toString(charset))
        }
      }
  }

  override def casUpdate(exec: ExecTrace): Try[ExecTrace] = ???

}
