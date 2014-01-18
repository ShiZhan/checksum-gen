package common

/**
 * @author ShiZhan
 * Archive file checker
 * for calculating md5 checksum from Archive Entries
 */
object ArchiveEx {
  import java.io.{ File, FileInputStream }
  import org.apache.commons.compress.archivers.ArchiveEntry
  import org.apache.commons.compress.archivers.zip.ZipFile
  import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
  import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
  import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
  import org.apache.commons.codec.digest.DigestUtils.md5Hex
  import DigestUtilsAddon.md5HexChunk

  case class ArcEntryChecksum(e: ArchiveEntry, arcivePath: String, checksum: String) {
    val path = e.getName
    val size = e.getSize
    override def toString = checksum + ';' + arcivePath + '#' + path + ';' + size
  }

  private def checkZip(file: File) = {
    try {
      val zf = new ZipFile(file)
      val entries = zf.getEntries
      val files = Iterator.continually {
        if (entries.hasMoreElements) entries.nextElement else null
      }.takeWhile(null !=).filter(!_.isDirectory)
      files map { e =>
        val is = zf.getInputStream(e)
        val md5 = md5Hex(is)
        is.close
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[ArcEntryChecksum]()
    }
  }

  private def checkGzip(file: File) = {
    val fis = new FileInputStream(file)
    val gzis = new GzipCompressorInputStream(fis)
    val tis = new TarArchiveInputStream(gzis)
    try {
      val files = Iterator.continually(tis.getNextTarEntry)
        .takeWhile(null !=).filter(_.isFile)
      files map { e =>
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[ArcEntryChecksum]()
    }
  }

  private def checkBz2(file: File) = {
    val fis = new FileInputStream(file)
    val bzis = new BZip2CompressorInputStream(fis)
    val tis = new TarArchiveInputStream(bzis)
    try {
      val files = Iterator.continually(tis.getNextTarEntry)
        .takeWhile(null !=).filter(_.isFile)
      files map { e =>
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[ArcEntryChecksum]()
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

    try {
      val zf = new SevenZFile(file)
      val entries = Iterator.continually { zf.getNextEntry }
        .takeWhile(null !=).filter(!_.isDirectory)
      entries map { e =>
        val size = e.getSize
        val md5 = md5Hex7Zip(zf, size)
        ArcEntryChecksum(e, file.getAbsolutePath, md5)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[ArcEntryChecksum]()
    }
  }

  type arcChecker = (File => Iterator[ArcEntryChecksum])
  private val arcCheckers = Map[String, arcChecker](
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
  private def defaultChecker(f: File) = Iterator[ArcEntryChecksum]()
  private val exts = arcCheckers map { case (k, c) => k } toSet
  def isKnownArchive(file: File) =
    (exts contains file.getName.split('.').last) & file.isFile
  def checkArc(file: File) = {
    val fileNameExtension = file.getName.split('.').last
    arcCheckers.getOrElse(fileNameExtension, defaultChecker _)(file)
  }
}