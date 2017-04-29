package hcube.scheduler.backend

import com.coreos.jetcd.EtcdClient
import com.coreos.jetcd.api.RangeRequest
import com.coreos.jetcd.op.{Cmp, CmpTarget, Op, Txn}
import com.coreos.jetcd.options.{DeleteOption, GetOption, PutOption}
import com.google.protobuf.ByteString
import com.typesafe.scalalogging.Logger
import hcube.scheduler.backend.Backend.{TransitionFailed, TransitionResult, TransitionSuccess}
import hcube.scheduler.model.{ExecTrace, JobSpec}
import hcube.scheduler.utils.JavaUtil._
import hcube.scheduler.utils.PathUtil

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EtcdBackend(
  etcd: EtcdClient,
  format: StorageFormat,
  path: String = "/hcube/scheduler",
  charset: String = "UTF-8"
)(
  implicit override val ec: ExecutionContext
) extends Backend {

  import EtcdBackend._

  private val MaxTimeKey = "9999999999999"
  private val MinTimeKey = "0000000000000"

  private val kvClient = etcd.getKVClient

  private val jobPath = PathUtil.join(path, "/job")
  private val jobPathBs = ByteString.copyFrom(jobPath, charset)

  private val jobEndPath = PathUtil.join(jobPath, "/zzz")
  private val jobEndPathBs = ByteString.copyFrom(jobEndPath, charset)

  private val statusPath = PathUtil.join(path, "/exec/status")
  private val tracePath = PathUtil.join(path, "/exec/trace")

  private val rangeOption = GetOption.newBuilder()
    .withSortField(RangeRequest.SortTarget.KEY)
    .withSortOrder(RangeRequest.SortOrder.ASCEND)
    .withRange(jobEndPathBs)
    .build()

  override def cleanup(jobId: String, numberOfJobsToPreserve: Int): Future[Long] = {

    val jobStatusPath = PathUtil.join(statusPath, jobId)
    val lastKeyToPreserve = kvClient.get(ByteString.copyFrom(jobStatusPath, charset),
      GetOption.newBuilder()
        .withKeysOnly(true)
        .withSortField(RangeRequest.SortTarget.KEY)
        .withSortOrder(RangeRequest.SortOrder.DESCEND)
        .withRange(ByteString.copyFrom(PathUtil.join(jobStatusPath, MaxTimeKey), charset))
        .withLimit(numberOfJobsToPreserve)
        .build()).toScalaFuture
      .map(response => {
        val keys = response.getKvsList
        if (keys.length == numberOfJobsToPreserve) keys.lastOption.map(_.getKey) else None
      })

    lastKeyToPreserve.flatMap((lastKey: Option[ByteString]) => {
      lastKey.map(key => {
        kvClient.delete(ByteString.copyFrom(PathUtil.join(jobStatusPath, MinTimeKey), charset),
          DeleteOption.newBuilder()
            .withRange(key)
            .build())
          .toScalaFuture.map(result => result.getDeleted)
      }).getOrElse(Future.successful(0L))
    })
  }

  override def pull(): Future[Seq[JobSpec]] = {
    kvClient
      .get(jobPathBs, rangeOption)
      .toScalaFuture
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
    val time = trace.time.toString
    val statusPathBs = ByteString.copyFrom(PathUtil.join(statusPath, jobId, time), charset)
    val tracePathBs = ByteString.copyFrom(PathUtil.join(tracePath, jobId, time), charset)

    val prevStatusBS = ByteString.copyFrom(prevState, charset)
    val newStatusBS = ByteString.copyFrom(newState, charset)

    val isExpectedPrev = if (prevState == "") {
      new Cmp(statusPathBs, Cmp.Op.EQUAL, CmpTarget.version(0))
    } else {
      new Cmp(statusPathBs, Cmp.Op.EQUAL, CmpTarget.value(prevStatusBS))
    }

    val txn = Txn.newBuilder().If(isExpectedPrev).Then(
      Op.put(statusPathBs, newStatusBS, PutOption.DEFAULT),
      Op.put(tracePathBs, ByteString.copyFrom(format.serialize(trace), charset), PutOption.DEFAULT)
    ).build()

    kvClient
      .commit(txn)
      .toScalaFuture
      .map { response =>
        if (response.getSucceeded && response.getResponsesCount == 2) {
          TransitionSuccess(trace)
        } else {
          TransitionFailed(trace)
        }
      }
  }

  override def put(job: JobSpec): Future[String] = {
    val key = PathUtil.join(jobPath, job.jobId)
    val bsKey = ByteString.copyFrom(key, charset)
    kvClient
      .put(bsKey, ByteString.copyFrom(format.serialize(job), charset), PutOption.DEFAULT)
      .toScalaFuture
      .map(_ => key)
  }

  override def delete(jobId: String): Future[String] = {
    val key = PathUtil.join(jobPath, jobId)
    val bsKey = ByteString.copyFrom(key, charset)
    kvClient
      .delete(bsKey, DeleteOption.DEFAULT)
      .toScalaFuture
      .map(_ => key)
  }

}

object EtcdBackend {

  val logger = Logger(getClass)

}
