package flyingwalrus.bytecask

import flyingwalrus.bytecask.Utils._
import flyingwalrus.bytecask.Bytes._
import java.util.concurrent.atomic.AtomicInteger

class Bytecask(
  val dir: String,
  val name: String = Utils.randomString(8),
  maxFileSize: Long = IO.DEFAULT_MAX_FILE_SIZE.toLong,
  maxConcurrentReaders: Int = 10,
  prefixedKeys: Boolean = false
) extends Logging
    with StateAware
    with Processors {

  val bytecask = this
  mkDirIfNeeded(dir)
  val io = new IO(dir, maxConcurrentReaders)
  val index = new Index(io, prefixedKeys)
  val splits = new AtomicInteger
  val merger = new Merger(io, index)
  val TOMBSTONE_VALUE = Bytes.EMPTY
  val id = Utils.uniqueId

  index.init()

  def put(key: Array[Byte], value: Array[Byte]) = {
    access {
      checkArgument(key.length > 0, "Key must not be empty")
      checkArgument(value.length > 0, "Value must not be empty")
      val entry = index.get(key)
      io.synchronized {
        val (pos, length, timestamp) = io.appendDataEntry(key, processor.before(value))
        if (entry.nonEmpty && entry.get.isInactive) merger.addReclaim(entry.get)
        index.update(key, pos, length, timestamp)
        if (io.pos > maxFileSize) split()
        putDone()
      }
    }
  }

  def get(key: Array[Byte]) = access {
    checkArgument(key.length > 0, "Key must not be empty")
    index.get(key) match {
      case Some(entry) => processor.after(Some(io.readValue(entry)))
      case _ => None
    }
  }

  def getMetadata(key: Array[Byte]) = access {
    checkArgument(key.length > 0, "Key must not be empty")
    index.get(key) match {
      case Some(entry) => Some(EntryMetadata(entry.length, entry.timestamp.toLong))
      case _ => None
    }
  }

  def delete(key: Array[Byte]) = access {
    checkArgument(key.length > 0, "Key must not be empty")
    index.get(key) match {
      case Some(entry) => {
        io.appendDataEntry(key, TOMBSTONE_VALUE)
        index.delete(key)
        if (entry.isInactive) merger.addReclaim(entry)
        deleteDone()
        entry
      }
      case _ => None
    }
  }

  def close() = {
    io.close()
  }

  def destroy() = {
    close()
    rmdir(dir)
  }

  def filesCount = ls(dir).size

  def stats(): String = access {
    "name: %s, dir: %s, uptime: %s, count: %s, splits: %s, merges: %s"
      .format(name, dir, now - createdAt, count(), splits.get(), merger.mergesCount)
  }

  protected def split() = {
    synchronized {
      index.postSplit(io.split())
    }
    splits.incrementAndGet()
  }

  def merge() = {
    synchronized {
      merger.forceMerge()
    }
  }

  /**
   * How much data out-of-date data might be reclaimed if merge triggered
   */

  def reclaimSize() = merger.reclaims.values.map(_.length).sum

  def reclaimPercentage() = (reclaimSize() / dirSize(dir)) * 100

  def count() = access {
    index.size
  }

  override def toString = s"$name, $dir"

  def keys() = access {
    index.keys
  }

  def values() = access {
    val iterator = index.keys.iterator
    new Iterator[Option[Bytes]]() {

      def hasNext = iterator.hasNext

      def next() = {
        val value = get(iterator.next())
        value.orElse(None)
      }
    }
  }

  def passivate() = {
    index.close()
    active.set(false)
  }

  def activate() = {
    index.init()
    active.set(true)
  }

  def idleTime = Utils.now - lastAccessed.get()

}

case class EntryMetadata(length: Int, timestamp: Long)

trait Processors {
  val processor: ValueProcessor = PassThru
}
