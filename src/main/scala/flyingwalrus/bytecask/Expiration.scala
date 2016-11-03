package flyingwalrus.bytecask

/**
 * Expiration is meant to provide auto-expiring entries that have exceeded the predefined TTL
 */

trait Expiration {

  val bytecask: Bytecask

  val ttl: Int //Seconds !

  def performExpiration() = {
    for (key <- bytecask.keys())
      for (meta <- bytecask.getMetadata(key))
        if (Utils.timestamp - meta.timestamp >= ttl)
          bytecask.delete(key)
  }

}
