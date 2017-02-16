package hcube.scheduler.backend

import scala.concurrent.ExecutionContext
import scala.util.Random

trait JobSpecShuffle extends Backend {

  implicit val ec: ExecutionContext

  abstract override def pullJobs() = {
    super.pullJobs().map(jobSpecs => Random.shuffle(jobSpecs))
  }
}
