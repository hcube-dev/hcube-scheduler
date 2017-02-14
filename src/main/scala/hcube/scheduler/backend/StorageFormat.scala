package hcube.scheduler.backend

import hcube.scheduler.model.JobSpec

trait StorageFormat {

  def serialize(spec: JobSpec): String

  def deserialize(str: String): JobSpec

}
