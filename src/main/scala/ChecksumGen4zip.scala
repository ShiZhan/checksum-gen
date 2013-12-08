object ChecksumGen4zip {

  import java.io.{ File, FileInputStream }
  import java.util.zip.{ ZipFile, ZipInputStream }
  import util.Hash

  private def zipMD5(file: File) = {
    try {
      val zf = new ZipFile(file)
      val zis = new ZipInputStream(new FileInputStream(file))
      val entries =
        Iterator.continually(zis.getNextEntry).takeWhile(null !=).filter(!_.isDirectory)
      entries map { e =>
        val path = e.getName
        val size = e.getSize
        val is = zf.getInputStream(e)
        val bytes = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte)
        val md5 = Hash.md5sum(bytes.toIterator)
        is.close
        md5Tuple(md5, path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
    }
  }

  def main(args: Array[String]) = {
    if (args.length < 1)
      println("usage: ChecksumGen4zip <zip file>")
    else
      zipMD5(new File(args(0))) foreach println
  }
}