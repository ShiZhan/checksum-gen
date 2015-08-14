/**
 * Archive file Operations
 */
package common

/**
 * @author ShiZhan
 * Archive file Operations
 */
object ArchiveEx {
  import java.io.{ File, FileInputStream, InputStream }
  import org.apache.commons.compress.archivers.ArchiveEntry
  import org.apache.commons.compress.archivers.zip.{ ZipArchiveEntry, ZipFile }
  import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
  import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
  import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
  import org.apache.commons.codec.digest.DigestUtils.md5Hex
  import DigestUtilsAddon.md5HexChunk

  implicit class InputStreamAsArchiveStream(is: InputStream) {
    def asGzip = new GzipCompressorInputStream(is)
    def asBzip = new BZip2CompressorInputStream(is)
    def asTar = new TarArchiveInputStream(is)
  }

  case class ArcEntryChecksum(e: ArchiveEntry, arcivePath: String, checksum: String) {
    val path = e.getName
    val size = e.getSize
    override def toString = s"$checksum;$arcivePath#$path;$size"
  }

  private def checkZip(file: File) = {
    val zf = new ZipFile(file)
    def getChecksum(e: ZipArchiveEntry) = {
      val is = zf.getInputStream(e)
      val md5 = md5Hex(is)
      is.close
      ArcEntryChecksum(e, file.getAbsolutePath, md5)
    }
    try {
      val entries = zf.getEntries
      Iterator.continually {
        if (entries.hasMoreElements) Some(entries.nextElement) else None
      }.takeWhile(_.isDefined).map(_.get).filterNot(_.isDirectory).map(getChecksum)
    } catch {
      case e: Exception => e.printStackTrace; Iterator.empty
    }
  }

  private def checkGzip(file: File) = {
    val fis = new FileInputStream(file)
    val gzis = new GzipCompressorInputStream(fis)
    val tis = new TarArchiveInputStream(gzis)
    try {
      val files = Iterator.continually(tis.getNextTarEntry)
        .takeWhile(_ != null).filter(_.isFile)
      files map { e =>
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator.empty
    }
  }

  private def checkBz2(file: File) = {
    val fis = new FileInputStream(file)
    val bzis = new BZip2CompressorInputStream(fis)
    val tis = new TarArchiveInputStream(bzis)
    try {
      val files = Iterator.continually(tis.getNextTarEntry)
        .takeWhile(_ != null).filter(_.isFile)
      files map { e =>
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator.empty
    }
  }

  private def check7Zip(file: File) = {
    import org.apache.commons.compress.archivers.sevenz.SevenZFile
    import org.apache.commons.codec.digest.DigestUtils.getDigest
    import org.apache.commons.codec.binary.Hex.encodeHexString

    val STREAM_BUFFER_LENGTH = 1024 * 64
    def md5Hex7Zip(data: SevenZFile, size: Long) = {
      val MD = getDigest("MD5")
      var buffer = new Array[Byte](STREAM_BUFFER_LENGTH)
      var total = 0
      val goal = size.toInt
      while (total < goal) {
        val read = data.read(buffer, 0, (goal - total) min STREAM_BUFFER_LENGTH)
        total += read
        MD.update(buffer, 0, read)
      }
      encodeHexString(MD.digest)
    }

    val zf = new SevenZFile(file)
    try {
      Iterator.continually(zf.getNextEntry)
        .takeWhile(_ != null).filterNot(_.isDirectory).map { e =>
          val size = e.getSize
          val md5 = md5Hex7Zip(zf, size)
          ArcEntryChecksum(e, file.getAbsolutePath, md5)
        }
    } catch {
      case e: Exception => e.printStackTrace; Iterator.empty
    }
  }

  private val arcCheckers = Map[String, File => Iterator[ArcEntryChecksum]](
    "zip" -> checkZip,
    "jar" -> checkZip,
    "war" -> checkZip,
    "apk" -> checkZip,
    "epub" -> checkZip,
    "odt" -> checkZip,
    "ods" -> checkZip,
    "odp" -> checkZip,
    "odg" -> checkZip,
    "docx" -> checkZip,
    "xlsx" -> checkZip,
    "pptx" -> checkZip,
    "tgz" -> checkGzip,
    "gz" -> checkGzip,
    "bz2" -> checkBz2,
    "7z" -> check7Zip)

  def getChecker(file: File) = arcCheckers.get(file.getName.split('.').last)
}
