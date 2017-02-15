package hcube.scheduler.backend

trait StorageFormat {

  def serialize(obj: AnyRef): String

  def deserialize[T](text: String)(implicit mf: scala.reflect.Manifest[T]): T

}
