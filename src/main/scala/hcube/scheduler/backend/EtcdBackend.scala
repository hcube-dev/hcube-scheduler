package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClient
import com.coreos.jetcd.api.RangeRequest
import com.coreos.jetcd.op.{Cmp, CmpTarget, Op, Txn}
import com.coreos.jetcd.options.{DeleteOption, GetOption, PutOption}
import com.google.protobuf.ByteString
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend.{TransitionFailed, TransitionResult, TransitionSuccess}
import hcube.scheduler.model.{ExecTrace, JobSpec}
import hcube.scheduler.utils.ListenableFutureUtil._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EtcdBackend(
  etcd: EtcdClient,
  format: StorageFormat,
  dir: String = "/hcube/scheduler",
  charset: String = "UTF-8"
)(
  implicit override val ec: ExecutionContext
) extends Backend {

  import EtcdBackend._

  private val MaxTimeKey = "9999999999999"
  private val MinTimeKey = "0000000000000"

  private val kvClient = etcd.getKVClient

  private val jobsKey = ByteString.copyFrom(dir + "/job/", charset)
  private val endKey = ByteString.copyFrom(dir + "/job/zzz", charset)
  private val getRangeOption = GetOption.newBuilder()
    .withSortField(RangeRequest.SortTarget.KEY)
    .withSortOrder(RangeRequest.SortOrder.ASCEND)
    .withRange(endKey)
    .build()

  override def removeOldJobs(jobId: String, numberOfJobsToPreserve: Int): Future[Long] = {

    val lastKeyToPreserve = kvClient.get(ByteString.copyFrom(statusPath(jobId), charset),
      GetOption.newBuilder()
        .withKeysOnly(true)
        .withSortField(RangeRequest.SortTarget.KEY)
        .withSortOrder(RangeRequest.SortOrder.DESCEND)
        .withRange(ByteString.copyFrom(statusPath(jobId) + MaxTimeKey, charset))
        .withLimit(numberOfJobsToPreserve)
        .build()).asScala
      .map(response => {
        val keys = response.getKvsList
        if (keys.length == numberOfJobsToPreserve) keys.lastOption.map(_.getKey) else None
      })

    lastKeyToPreserve.flatMap((lastKey: Option[ByteString]) => {
      lastKey.map(key => {
        kvClient.delete(ByteString.copyFrom(statusPath(jobId) + MinTimeKey, charset),
          DeleteOption.newBuilder()
            .withRange(key)
            .build())
          .asScala.map(result => result.getDeleted)
      }).getOrElse(Future.successful(0L))
    })
  }

  override def pullJobs(): Future[Seq[JobSpec]] = {
    kvClient
      .get(jobsKey, getRangeOption)
      .asScala
      .map { response =>
        response.getKvsList.toList.flatMap { kv =>
          val json = kv.getValue.toString(charset)
          Try(format.deserialize[JobSpec](json)) match {
            case Success(jobSpec) => Some(jobSpec)
            case Failure(e) =>
              logger.error(s"Failed deserializing job: $json", e)
              None
          }
        }
      }
  }

  override def transition(
    prevState: String,
    newState: String,
    trace: ExecTrace
  ): Future[TransitionResult] = {
    val jobId = trace.jobId
    val time = trace.time
    val keyStatus = ByteString.copyFrom(dir + s"/exec/status/$jobId/$time", charset)
    val keyTrace = ByteString.copyFrom(dir + s"/exec/trace/$jobId/$time", charset)

    val prevStatusBS = ByteString.copyFrom(prevState, charset)
    val newStatusBS = ByteString.copyFrom(newState, charset)

    val isExpectedPrev = if (prevState == "") {
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
        if (response.getSucceeded && response.getResponsesCount == 2) {
          TransitionSuccess(trace)
        } else {
          TransitionFailed(trace)
        }
      }
  }

  private def statusPath(jobId: String) = dir + s"/exec/status/$jobId/"

}

object EtcdBackend {

  val logger = Logger(getClass)

}
