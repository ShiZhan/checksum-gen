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

  import scala.io.Source
  import java.io.{ File, InputStream, FileInputStream, BufferedInputStream }
  import org.apache.commons.compress.archivers.zip.ZipFile
  import org.apache.commons.compress.archivers.ArchiveStreamFactory
  import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
  import org.apache.commons.compress.archivers.tar.TarArchiveEntry
  import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
  import org.apache.commons.codec.digest.DigestUtils.md5Hex

  private def md5HexChunk(is: InputStream, size: Int) = {

  }

  private def checkFile(file: File) = {
    val fIS = new BufferedInputStream(new FileInputStream(file))
    val md5 = md5Hex(fIS)
    fIS.close
    md5Tuple(md5, file.getAbsolutePath, file.length)
  }

  private def checkChunk(file: File, chunkSize: Int) = {
    val size = file.length
    if (size > chunkSize) {
      val fileBuffer = Source.fromFile(file, "ISO-8859-1")
      val chunks = fileBuffer.map(_.toByte).grouped(chunkSize)
      val md5Array = chunks.map { bytes => md5Hex(bytes.toArray) }.toArray
      fileBuffer.close
      val lastChunk = size / chunkSize
      val lastSize = size % chunkSize
      val path = file.getAbsolutePath
      md5Array.zipWithIndex.map {
        case (m, i) =>
          md5Tuple(m, path + "." + i, if (i == lastChunk) lastSize else chunkSize)
      }
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
        md5Tuple("WIP", path, size)
      }
    } catch {
      case e: Exception => e.printStackTrace; Iterator[md5Tuple]()
    }
  }

  /*
   * @TODO: handler for bz2
   */
  private def checkBz2(file: File) = {
    Iterator[md5Tuple]()
  }

  /*
   * @TODO: handler for 7zip
   */
  private def check7Zip(file: File) = {
    Iterator[md5Tuple]()
  }

  /*
   * @TODO: handler for all archives with extension detection
   */
  private def checkArc(file: File) = {
    val name = file.getName
    val ext = name.substring(name.lastIndexOf(".") + 1)
    ext match {
      case "zip" => checkZip(file)
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
        val chunkSize = chunkSizeStr.toInt
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