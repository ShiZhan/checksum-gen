object ChecksumGen {

  import scala.io.Source
  import java.io.File
  import java.security.MessageDigest

  def md5(bytes: Iterator[Byte]) = {
    val md5 = MessageDigest.getInstance("MD5")
    try {
      bytes.foreach(md5.update)
      //md5.update(bytes.toArray)
    } catch {
      case e: Exception => throw e
    }
    md5.digest.map("%02x".format(_)).mkString
  }

  type md5Tuple = (String, String, Long)

  private def fileMD5(file: File) = {
    val fileBuffer = Source.fromFile(file, "ISO-8859-1")
    val fileBytes = fileBuffer.map(_.toByte)
    val md5sum = md5(fileBytes)
    fileBuffer.close
    (md5sum, file.getAbsolutePath, file.length)
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
          (m, path + "." + i, if (i == lastChunk) lastSize else chunkSize)
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
          fileMD5(source) match { case (m, p, s) => println(m + ';' + p + ';' + s) }
        else {
          val md5list = collect(source)
          for ((m, p, s) <- md5list) println(m + ';' + p + ';' + s)
        }
      }
      case fileName :: chunkSizeStr :: Nil => {
        val source = new File(fileName)
        val chunkSize = chunkSizeStr.toInt
        if (!source.exists) println("input source does not exist")
        else if (source.isFile)
          chunkMD5(source, chunkSize) foreach {
            case (m, p, s) => println(m + ';' + p + ';' + s)
          }
        else {
          val md5tree = collect(source, chunkSize)
          val md5list = md5tree.flatMap { case (f, c) => Array(f) ++ c }
          for ((m, p, s) <- md5list) println(m + ';' + p + ';' + s)
        }
      }
      case _ => println("usage: ChecksumGen <source> [<chunk size>]")
    }
  }

}