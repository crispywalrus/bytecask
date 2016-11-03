package flyingwalrus.bytecask

/**
 * Eviction to keep max N entries and remove the oldest as first (LIFO)
 */

trait Eviction {

  val bytecask: Bytecask

  val maxCount: Int

  /*
    If there are items beyond the limit, find the oldest and remove
 */

  def performEviction() = {
    if (maxCount > 0) {
      val toEvict = bytecask.count() - maxCount
      if (toEvict > 0) {
        val items = bytecask.keys().map(key => (key, bytecask.getMetadata(key).get.timestamp)).toArray.sortBy(t => t._2)
        for (item <- items.take(toEvict))
          bytecask.delete(item._1)
      }
    }
  }


}
