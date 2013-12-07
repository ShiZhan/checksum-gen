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
  import java.io.File
  import java.security.MessageDigest

  private def md5(bytes: Iterator[Byte]) = {
    val md5 = MessageDigest.getInstance("MD5")
    try {
      bytes.foreach(md5.update)
    } catch {
      case e: Exception => throw e
    }
    md5.digest.map("%02x".format(_)).mkString
  }

  private def fileMD5(file: File) = {
    val fileBuffer = Source.fromFile(file, "ISO-8859-1")
    val fileBytes = fileBuffer.map(_.toByte)
    val md5sum = md5(fileBytes)
    fileBuffer.close
    md5Tuple(md5sum, file.getAbsolutePath, file.length)
  }

  private def chunkMD5(file: File, chunkSize: Int) = {
    val size = file.length
    if (size > chunkSize) {
      val fileBuffer = Source.fromFile(file, "ISO-8859-1")
      val fileBytes = fileBuffer.map(_.toByte)
      val chunks = fileBytes.grouped(chunkSize).map(_.iterator)
      val md5sumArray = chunks.map(md5).toArray
      fileBuffer.close
      val lastChunk = size / chunkSize
      val lastSize = size % chunkSize
      val path = file.getAbsolutePath
      md5sumArray.zipWithIndex.map {
        case (m, i) =>
          md5Tuple(m, path + "." + i, if (i == lastChunk) lastSize else chunkSize)
      }
    } else
      Array[md5Tuple]()
  }

  private def collect(dir: File, chunkSize: Int) = {

    def checkDir(d: File): Array[(md5Tuple, Array[md5Tuple])] = {
      val (files, dirs) = d.listFiles.partition(_.isFile)
      val md5Files = files.map { f => (fileMD5(f), chunkMD5(f, chunkSize)) }
      md5Files ++ dirs.flatMap(checkDir)
    }

    checkDir(dir)
  }

  private def collect(dir: File) = {

    def checkDir(d: File): Array[md5Tuple] = {
      val (files, dirs) = d.listFiles.partition(_.isFile)
      val md5Files = files.map { fileMD5 }
      md5Files ++ dirs.flatMap(checkDir)
    }

    checkDir(dir)
  }

  def main(args: Array[String]) = {
    args.toList match {
      case fileName :: Nil => {
        val source = new File(fileName)
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          println(fileMD5(source))
        else
          collect(source) foreach println
      }
      case fileName :: chunkSizeStr :: Nil => {
        val source = new File(fileName)
        val chunkSize = chunkSizeStr.toInt
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          chunkMD5(source, chunkSize) foreach println
        else {
          collect(source, chunkSize) foreach {
            case (f, c) => println(f); c foreach println
          }
        }
      }
      case _ => println("usage: ChecksumGen <source> [<chunk size>]")
    }
  }

}

object ChecksumGen4zip {

  import java.io.{ File, FileInputStream }
  import java.util.zip.{ ZipEntry, ZipException, ZipFile, ZipInputStream }

  private def zipMD5(file: File) = {
    try {
      val zf = new ZipFile(file)
      val zis = new ZipInputStream(new FileInputStream(file))
      val entries = Iterator.continually { zis.getNextEntry } takeWhile(null != )
      for (e <- entries) {
        val path = e.getName
        val is = zf.getInputStream(e)
        
        is.close
      }
      Array[md5Tuple]()
    } catch {
      case e: Exception => e.printStackTrace; Array[md5Tuple]()
    }
  }

  def main(args: Array[String]) = {

  }
}