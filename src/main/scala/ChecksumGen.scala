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
  import common.ArchiveEx._
  import common.FileEx._

  val usage = """usage: ChecksumGen [-a|-c <chunk size>] <directory|file>
    -a: compressed file only
    -c: list all chunk checksums for those larger than chunk size"""

  def main(args: Array[String]) = args.toList match {
    case "-a" :: fileNames =>
      for (
        fileName <- fileNames;
        f <- fileName.toFile.flatten if f.isFile;
        c = getChecker(f);
        e <- c.get(f) if c.isDefined
      ) println(e)
    case "-c" :: chunkSizeStr :: fileNames =>
      for (
        fileName <- fileNames;
        f <- fileName.toFile.flatten if f.isFile;
        (i, s, m) <- f.checksum(chunkSizeStr.toLong);
        e = s"$m;${f.getAbsolutePath}.$i;$s"
      ) println(e)
    case fileNames if fileNames.nonEmpty =>
      for (
        fileName <- fileNames;
        f <- fileName.toFile.flatten if f.isFile;
        e = s"${f.checksum};${f.getAbsolutePath};${f.length}"
      ) println(e)
    case _ => println(usage)
  }
}
