/**
 * Copyright 2013 Zhan Shi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
object DigestUtilsAddon {
  import java.io.InputStream
  import org.apache.commons.codec.digest.DigestUtils.getDigest
  import org.apache.commons.codec.binary.Hex.encodeHexString

  private val STREAM_BUFFER_LENGTH = 1024 * 64
  def md5HexChunk(data: InputStream, size: Long) = {
    val MD = getDigest("MD5")
    var buffer = new Array[Byte](STREAM_BUFFER_LENGTH)
    var total = 0
    val goal = size.toInt min data.available
    while (total < goal) {
      val read = data.read(buffer, 0, (goal - total) min STREAM_BUFFER_LENGTH)
      total += read
      MD.update(buffer, 0, read)
    }
    encodeHexString(MD.digest)
  }
}

object ArchiveCheckers {
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
  def extKnown(file: File) = exts contains file.getName.split('.').last
  def checkArc(file: File) = {
    assert(file.isFile)
    val fileNameExtension = file.getName.split('.').last
    arcCheckers.getOrElse(fileNameExtension, defaultChecker _)(file)
  }
}

object FileCheckers {
  import java.io.{ File, FileInputStream, BufferedInputStream }
  import org.apache.commons.codec.digest.DigestUtils.md5Hex
  import DigestUtilsAddon.md5HexChunk

  case class md5Tuple(md5sum: String, path: String, size: Long) {
    override def toString = md5sum + ';' + path + ';' + size
  }

  def checkFile(file: File) = {
    val fIS = new BufferedInputStream(new FileInputStream(file))
    val md5 = md5Hex(fIS)
    fIS.close
    md5Tuple(md5, file.getAbsolutePath, file.length)
  }

  def checkChunk(file: File, chunkSize: Long) = {
    val fileSize = file.length
    if (fileSize > chunkSize) {
      val indexOfLastChunk = fileSize / chunkSize
      val sizeOfLastChunk = fileSize % chunkSize
      val fileAbsolutePath = file.getAbsolutePath
      val fileInputStream = new BufferedInputStream(new FileInputStream(file))
      val md5Array = (0 to indexOfLastChunk.toInt).map { i =>
        val md5 = md5HexChunk(fileInputStream, chunkSize)
        val path = fileAbsolutePath + "." + i
        val size = if (i == indexOfLastChunk) sizeOfLastChunk else chunkSize
        md5Tuple(md5, path, size)
      }.toArray
      fileInputStream.close
      md5Array
    } else
      Array[md5Tuple]()
  }
}

object ChecksumGen {

  import java.io.File
  import FileCheckers.{ checkFile, checkChunk }
  import ArchiveCheckers.{ extKnown, checkArc }

  private def listAllFiles(dir: File): Array[File] = {
    assert(dir.isDirectory)
    val list = dir.listFiles
    list ++ list.filter(_.isDirectory).flatMap(listAllFiles)
  }

  val usage = """usage: ChecksumGen [-a|-c <chunk size>] <source>
    -a: open <source> as compressed file,
        or find & check all compressed file in <source> directory.
    -c <chunk size>: check designated file or all files in directory,
        list all chunk checksums for those larger than chunk size,
        files are not actually chunked."""

  def main(args: Array[String]) = {
    args.toList match {
      case fileName :: Nil => {
        val source = new File(fileName)
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          println(checkFile(source))
        else
          listAllFiles(source).foreach { f =>
            if (f.isFile) println(checkFile(f))
          }
      }
      case "-a" :: fileName :: Nil => {
        val source = new File(fileName)
        if (!source.exists) println("input source does not exist")
        else if (source.isFile) {
          if (extKnown(source))
            checkArc(source) foreach println
          else
            println("Unknown archive format")
        } else {
          listAllFiles(source).foreach { f =>
            if (f.isFile & extKnown(f)) checkArc(f) foreach println
          }
        }
      }
      case "-c" :: chunkSizeStr :: fileName :: Nil => {
        val source = new File(fileName)
        val chunkSize = chunkSizeStr.toLong
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          checkChunk(source, chunkSize) foreach println
        else {
          listAllFiles(source).foreach { f =>
            if (f.isFile) checkChunk(f, chunkSize) foreach println
          }
        }
      }
      case _ => println(usage)
    }
  }

}