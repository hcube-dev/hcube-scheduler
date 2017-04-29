package hcube.scheduler.utils

object PathUtil {

  def join(parts: String*): String = parts.mkString("/").replaceAll("//+", "/")

}
