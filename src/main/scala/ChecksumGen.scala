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
object ChecksumGen {
  import java.io.File
  import helper.ArchiveCheckers.{ extKnown, checkArc }
  import helper.FileEx.FileOps

  val usage = """usage: ChecksumGen [-a|-c <chunk size>] <source>
    -a: treat <source> as compressed file or folder contains them.
    -c <chunk size>: check designated file or all files in directory,
        list all chunk checksums for those larger than chunk size,
        files are not actually chunked."""

  def main(args: Array[String]) = {
    args.toList match {
      case fileName :: Nil => {
        val source = new File(fileName)
        source.flatten.foreach { f =>
          if (f.isFile)
            println(f.checksum + "; " + f.getAbsolutePath + "; " + f.length)
        }
      }
      case "-a" :: fileName :: Nil => {
        val source = new File(fileName)
        source.flatten.foreach { f =>
          if (f.isFile & extKnown(f)) checkArc(f) foreach println
        }
      }
      case "-c" :: chunkSizeStr :: fileName :: Nil => {
        val source = new File(fileName)
        val chunkSize = chunkSizeStr.toLong
        source.flatten.foreach { f =>
          if (f.isFile) f.checksum(chunkSize).map {
            case (i, s, m) => m + "; " + f.getAbsolutePath + '.' + i + "; " + s
          } foreach println
        }
      }
      case _ => println(usage)
    }
  }
}