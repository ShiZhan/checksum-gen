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
case class md5Tuple(md5sum: String, path: String, size: Long) {
  override def toString = md5sum + ';' + path + ';' + size
}

object ChecksumGen {

  import java.io.{ File, InputStream, FileInputStream, BufferedInputStream }
  import org.apache.commons.compress.archivers.zip.ZipFile
  import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
  import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
  import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
  import org.apache.commons.codec.digest.DigestUtils.{ getDigest, md5Hex }
  import org.apache.commons.codec.binary.Hex.encodeHexString

  val STREAM_BUFFER_LENGTH = 1024 * 64
  private def md5HexChunk(data: InputStream, size: Long) = {
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

  private def checkFile(file: File) = {
    val fIS = new BufferedInputStream(new FileInputStream(file))
    val md5 = md5Hex(fIS)
    fIS.close
    md5Tuple(md5, file.getAbsolutePath, file.length)
  }

  private def checkChunk(file: File, chunkSize: Long) = {
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

  private def checkZip(file: File) = {
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

  private def checkGzip(file: File) = {
    val fis = new FileInputStream(file)
    val gzis = new GzipCompressorInputStream(fis)
    val tis = new TarArchiveInputStream(gzis)
    try {
      val files = Iterator.continually(tis.getNextTarEntry)
        .takeWhile(null !=).filter(_.isFile)
      files map { e =>
        val path = e.getName
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        md5Tuple(md5, path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
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
        val path = e.getName
        val size = e.getSize
        val md5 = md5HexChunk(tis, size)
        md5Tuple(md5, path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
    }
  }

  /*
   * @TODO: handler for 7zip
   */
  private def check7Zip(file: File) = {
    Iterator[md5Tuple]()
  }

  private def checkArc(file: File) = {
    val fileNameExtension = file.getName.split('.').last
    fileNameExtension match {
      case "zip" => checkZip(file)
      case "jar" => checkZip(file)
      case "war" => checkZip(file)
      case "apk" => checkZip(file)
      case "tgz" => checkGzip(file)
      case "gz" => checkGzip(file)
      case "bz2" => checkBz2(file)
      case "7z" => check7Zip(file)
      case _ => Iterator[md5Tuple]()
    }
  }

  private def listAllFiles(dir: File): Array[File] = {
    assert(dir.isDirectory)
    val list = dir.listFiles
    list ++ list.filter(_.isDirectory).flatMap(listAllFiles)
  }

  def main(args: Array[String]) = {
    args.toList match {
      case fileName :: Nil => {
        val source = new File(fileName)
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          println(checkFile(source))
        else
          listAllFiles(source).filter(_.isFile) foreach { f => println(checkFile(f)) }
      }
      case fileName :: "--zip" :: Nil => {
        val source = new File(fileName)
        if (!source.exists) println("input source does not exist")
        else if (source.isFile) {
          checkArc(source) foreach println
        } else
          println("input source is not a file")
      }
      case fileName :: chunkSizeStr :: Nil => {
        val source = new File(fileName)
        val chunkSize = chunkSizeStr.toLong
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          checkChunk(source, chunkSize) foreach println
        else {
          listAllFiles(source).filter(_.isFile) foreach { f =>
            println(checkFile(f))
            checkChunk(f, chunkSize) foreach println
          }
        }
      }
      case _ => println("usage: ChecksumGen <source> [<chunk size> <--zip>]")
    }
  }

}