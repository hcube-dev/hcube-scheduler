package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClient
import com.coreos.jetcd.op.{Cmp, CmpTarget, Op, Txn}
import com.coreos.jetcd.options.{GetOption, PutOption}
import com.google.protobuf.ByteString
import hcube.scheduler.model.{ExecTrace, JobSpec}
import hcube.scheduler.utils.ListenableFutureUtil._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class EtcdBackend(
  etcd: EtcdClient,
  format: StorageFormat,
  dir: String = "/hcube/scheduler",
  charset: String = "UTF-8"
)(
  implicit ec: ExecutionContext
) extends Backend {

  private val kvClient = etcd.getKVClient

  private val jobsKey = ByteString.copyFrom(dir + "/job/", charset)
  private val endKey = ByteString.copyFrom(dir + "/job/ZZZ", charset)
  private val getRangeOption = GetOption.newBuilder().withRange(endKey).build()

  private val successValue = ByteString.copyFrom("success", charset)

  override def pullJobs(): Future[Seq[JobSpec]] = {
    kvClient
      .get(jobsKey, getRangeOption)
      .asScala
      .map { response =>
        response.getKvsList.toList.map { kv =>
          format.deserialize[JobSpec](kv.getValue.toString(charset))
        }
      }
  }

  override def updateExecTxn(exec: ExecTrace): Future[UpdateResponse] = {
    val jobId = exec.jobId
    val time = exec.time.toEpochMilli
    val keyStatus = ByteString.copyFrom(dir + s"/exec/status/$jobId/$time", charset)
    val keyTrace = ByteString.copyFrom(dir + s"/exec/trace/$jobId/$time", charset)
    val isSuccess = new Cmp(keyStatus, Cmp.Op.EQUAL, CmpTarget.value(successValue))

    val txn = Txn.newBuilder().If(isSuccess).Else(
      Op.put(keyStatus, successValue, PutOption.DEFAULT),
      Op.put(keyTrace, ByteString.copyFrom(format.serialize(exec), charset), PutOption.DEFAULT)
    ).build()

    kvClient
      .commit(txn)
      .asScala
      .map { response =>
        val success = !response.getSucceeded && response.getResponsesCount == 2
        UpdateResponse(success, exec)
      }
  }

}
