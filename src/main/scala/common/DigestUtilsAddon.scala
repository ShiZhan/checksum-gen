/**
 * Apache commons-codec DigestUtils Addon
 */
package common

/**
 * @author ShiZhan
 * Apache commons-codec DigestUtils Addon
 * for calculating md5 checksum from InputStream segment
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