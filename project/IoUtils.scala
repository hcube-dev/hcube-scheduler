import sbt._

object IoUtils {
  def copyIfChanged(src: PathFinder, dest: File, logger: String => Unit = (msg) => {}) {
    if (!dest.exists()) {
      IO.createDirectory(dest)
    } else if (!dest.isDirectory) {
      throw new IllegalStateException(s"Destination is not a directory: $dest")
    }
    src.get.foreach(file => {
      val fileName = file.getName
      val destFile =  dest / fileName
      if (!destFile.exists() || destFile.lastModified() < file.lastModified()) {
        logger(s"Copying file $fileName to $dest")
        IO.copyFile(file, destFile, preserveLastModified = true)
      }
    })
  }

  def append(dest: File, src: File) = IO.append(dest,
    scala.io.Source.fromFile(src.toURI).mkString)

}