case class md5Tuple(md5sum: String, path: String, size: Long) {
  override def toString = md5sum + ';' + path + ';' + size
}