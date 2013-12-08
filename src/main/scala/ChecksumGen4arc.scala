object ChecksumGen4arc {

  import java.io.{ File, InputStream }
  import org.apache.commons.compress.archivers.zip.ZipFile
  import org.apache.commons.codec.digest.DigestUtils

  private def md5sum(is: InputStream) =
    DigestUtils.md5(is).map("%02x".format(_)).mkString

  private def zipMD5(file: File) = {
    try {
      val zf = new ZipFile(file)
      val entries = zf.getEntries
      val files = Iterator.continually(entries.nextElement)
        .takeWhile(e => entries.hasMoreElements)
        .filter(!_.isDirectory)
      files map { e =>
        val path = e.getName
        val size = e.getSize
        val is = zf.getInputStream(e)
        val md5 = md5sum(is)
        is.close
        md5Tuple(md5, path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
    }
  }

  def main(args: Array[String]) = {
    if (args.length < 1)
      println("usage: ChecksumGen4arc <zip|jar|apk file>")
    else
      zipMD5(new File(args(0))) foreach println
  }
}