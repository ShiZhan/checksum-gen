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
  import common.ArchiveEx.{ isKnownArchive, checkArc }
  import common.FileEx.FileOps

  val usage = """usage: ChecksumGen [-a|-c <chunk size>] <directory|file>
    -a: compressed file only
    -c: list all chunk checksums for those larger than chunk size"""

  def main(args: Array[String]) = {
    args.toList match {
      case fileName :: Nil => new File(fileName).flatten.foreach { f =>
        if (f.isFile) println(f.checksum + ';' + f.getAbsolutePath + ';' + f.length)
      }
      case "-a" :: fileName :: Nil => new File(fileName).flatten.foreach { f =>
        if (isKnownArchive(f)) checkArc(f) foreach println
      }
      case "-c" :: chunkSizeStr :: fileName :: Nil => {
        val chunkSize = chunkSizeStr.toLong
        new File(fileName).flatten.foreach { f =>
          if (f.isFile) f.checksum(chunkSize).map {
            case (i, s, m) => m + ';' + f.getAbsolutePath + '.' + i + ';' + s
          } foreach println
        }
      }
      case _ => println(usage)
    }
  }
}