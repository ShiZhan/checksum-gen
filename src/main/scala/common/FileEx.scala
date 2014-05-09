/**
 * Additional File methods
 */
package common

/**
 * @author ShiZhan
 * Additional File methods
 */
object FileEx {
  import java.io._
  import org.apache.commons.codec.digest.DigestUtils.md5Hex
  import DigestUtilsAddon.md5HexChunk

  implicit class FileName(name: String) {
    def toFile = new File(name)
    def setExt(ext: String) =
      if (name.split('.').last == ext) name else name + '.' + ext
  }

  private def listAllFiles(file: File): Array[File] = {
    val list = file.listFiles
    if (list == null)
      Array[File]()
    else
      list ++ list.filter(_.isDirectory).flatMap(listAllFiles)
  }

  implicit class FileOps(file: File) {
    def flatten =
      if (file.exists)
        if (file.isDirectory) listAllFiles(file) else Array(file)
      else
        Array[File]()

    def checksum =
      try {
        val fIS = new BufferedInputStream(new FileInputStream(file))
        val md5 = md5Hex(fIS)
        fIS.close
        md5
      } catch {
        case e: Exception => ""
      }

    def checksum(chunkSize: Long) =
      try {
        val fileSize = file.length
        if (fileSize > chunkSize) {
          val lastChunkIndex = (fileSize / chunkSize).toInt
          val lastChunkSize = fileSize % chunkSize
          val fileInputStream = new BufferedInputStream(new FileInputStream(file))
          val md5Array = (0 to lastChunkIndex).map { i =>
            val size = if (i == lastChunkIndex) lastChunkSize else chunkSize
            val md5 = md5HexChunk(fileInputStream, chunkSize)
            (i, size, md5)
          }.toArray
          fileInputStream.close
          md5Array
        } else
          Array[(Int, Long, String)]()
      } catch {
        case e: Exception => Array[(Int, Long, String)]()
      }

    def getWriter =
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))

    def getReader =
      new BufferedReader(new InputStreamReader(new FileInputStream(file)))

    def getWriter(coding: String) =
      new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), coding))

    def getReader(coding: String) =
      new BufferedReader(new InputStreamReader(new FileInputStream(file), coding))
  }
}