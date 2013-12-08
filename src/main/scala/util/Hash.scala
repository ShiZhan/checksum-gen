/**
 * MessageDigest functions
 */
package util

import java.security.MessageDigest

/**
 * @author ShiZhan
 * Java MessageDigest wrapper
 */
object Hash {

  private val md5 = MessageDigest.getInstance("MD5")
  private val sha = MessageDigest.getInstance("SHA-1")
  private val sha256 = MessageDigest.getInstance("SHA-256")

  def md5sum(bytes: Iterator[Byte]) = {
    md5.reset
    try { bytes.foreach(md5.update) }
    catch { case e: Exception => throw e }
    md5.digest.map("%02x".format(_)).mkString
  }

}