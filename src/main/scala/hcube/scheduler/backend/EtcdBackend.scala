package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClient
import com.coreos.jetcd.api.RangeRequest
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
  implicit override val ec: ExecutionContext
) extends Backend {

  private val kvClient = etcd.getKVClient

  private val jobsKey = ByteString.copyFrom(dir + "/job/", charset)
  private val endKey = ByteString.copyFrom(dir + "/job/zzz", charset)
  private val getRangeOption = GetOption.newBuilder()
    .withSortField(RangeRequest.SortTarget.KEY)
    .withSortOrder(RangeRequest.SortOrder.ASCEND)
    .withRange(endKey)
    .build()

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

  override def transitionCAS(
    prevStatus: String,
    newStatus: String,
    trace: ExecTrace
  ): Future[UpdateResponse] = {
    val jobId = trace.jobId
    val time = trace.time.toEpochMilli
    val keyStatus = ByteString.copyFrom(dir + s"/exec/status/$jobId/$time", charset)
    val keyTrace = ByteString.copyFrom(dir + s"/exec/trace/$jobId/$time", charset)

    val prevStatusBS = ByteString.copyFrom(prevStatus, charset)
    val newStatusBS = ByteString.copyFrom(newStatus, charset)

    val isExpectedPrev = if (prevStatus == "") {
      new Cmp(keyStatus, Cmp.Op.EQUAL, CmpTarget.version(0))
    } else {
      new Cmp(keyStatus, Cmp.Op.EQUAL, CmpTarget.value(prevStatusBS))
    }

    val txn = Txn.newBuilder().If(isExpectedPrev).Then(
      Op.put(keyStatus, newStatusBS, PutOption.DEFAULT),
      Op.put(keyTrace, ByteString.copyFrom(format.serialize(trace), charset), PutOption.DEFAULT)
    ).build()

    kvClient
      .commit(txn)
      .asScala
      .map { response =>
        val success = response.getSucceeded && response.getResponsesCount == 2
        UpdateResponse(success, trace)
      }
  }

}
