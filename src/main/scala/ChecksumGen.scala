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

import scala.io.Source
import java.io.{ File, FileInputStream, BufferedInputStream, ByteArrayInputStream }
import org.apache.commons.codec.digest.DigestUtils.md5Hex

object ChecksumGen {

  private def fileMD5(file: File) = {
    val fIS = new BufferedInputStream(new FileInputStream(file))
    val md5 = md5Hex(fIS)
    fIS.close
    md5Tuple(md5, file.getAbsolutePath, file.length)
  }

  private def chunkMD5(file: File, chunkSize: Int) = {
    val size = file.length
    if (size > chunkSize) {
      val fileBuffer = Source.fromFile(file, "ISO-8859-1")
      val fileBytes = fileBuffer.map(_.toByte)
      val chunks = fileBytes.grouped(chunkSize).map { b =>
        new ByteArrayInputStream(b.toArray)
      }
      val md5Array = chunks.map(md5Hex).toArray
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