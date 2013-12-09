object ChecksumGen4zip {

  import java.io.File
  import org.apache.commons.compress.archivers.zip.ZipFile
  import org.apache.commons.codec.digest.DigestUtils.md5Hex

  private def zipMD5(file: File) = {
    try {
      val zf = new ZipFile(file)
      val entries = zf.getEntries
      val files = Iterator.continually {
        if (entries.hasMoreElements) entries.nextElement else null
      }.takeWhile(null !=).filter(!_.isDirectory)
      files map { e =>
        val path = e.getName
        val size = e.getSize
        val is = zf.getInputStream(e)
        val md5 = md5Hex(is)
        is.close
        md5Tuple(md5, path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
    }
  }

  def main(args: Array[String]) = {
    if (args.length < 1)
      println("usage: ChecksumGen4zip <zip|jar|apk file>")
    else
      zipMD5(new File(args(0))) foreach println
  }
}