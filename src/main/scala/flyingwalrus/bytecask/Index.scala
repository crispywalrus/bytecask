package flyingwalrus.bytecask

import collection.mutable.Map
import java.io.File

import flyingwalrus.bytecask.Utils._
import flyingwalrus.bytecask.Bytes._


/*
Index (aka Keydir)- keeps position and length of entry in a file
 */

final class Index(io: IO, prefixedKeys: Boolean = false) extends Logging with Locking with Tracking {

  val indexMap = if (prefixedKeys) new PrefixIndexMap else Map[Bytes, IndexEntry]()

  def init() = {
    synchronized {
      debug("Initializing index...")
      if (ls(io.dir).toList.filter(_.length() > 0).map(readData).filter(!_).size > 0) incErrors
    }
  }

  def close() = {
    synchronized {
      indexMap.clear()
    }
    System.gc()
  }

  private def hintFile(file: File) = (file.getAbsolutePath + "h").mkFile

  private def readData(file: File) = {
    val hf = hintFile(file)
    if (hf.exists()) {
      debug("hint file exists for " + file)
      IO.readHintEntries(hf, processHintEntry)
    } else IO.readDataEntries(file, processDataEntry)
  }

  private def processDataEntry(file: File, entry: DataEntry) = writeLock {
    if (entry.valueSize == 0)
      indexMap.remove(entry.key)
    else
      indexMap.put(entry.key, IndexEntry(file.getName, entry.pos, entry.size, entry.timestamp))
  }

  private def processHintEntry(file: File, entry: HintEntry) = writeLock {
    if (entry.valueSize == 0)
      indexMap.remove(entry.key)
    else
      indexMap.put(entry.key, IndexEntry(file.getName, entry.pos, entry.size, entry.timestamp))
  }

  def update(key: Bytes, pos: Int, length: Int, timestamp: Int) = writeLock {
    indexMap.put(key, IndexEntry(IO.ACTIVE_FILE_NAME, pos, length, timestamp))
  }

  def get(key: Bytes) = readLock {
    indexMap.get(key)
  }

  def delete(key: Bytes) = writeLock {
    indexMap.remove(key)
  }

  def postSplit(file: String) = {
    writeLock {
      for ((key, entry) <- indexMap; if (entry.isActive)) {
        indexMap.put(key, IndexEntry(file, entry.pos, entry.length, entry.timestamp))
      }
    }
  }

  def hasEntry(entry: DataEntry) = {
    val e = indexMap.get(entry.key)
    //debug("hasEntry: " + e + " -> " + entry)
    e.nonEmpty && e.get.timestamp == entry.timestamp
  }

  def contains(key: Bytes) = indexMap.contains(key)

  def size = indexMap.size

  def keys = indexMap.keys

  def getMap = indexMap
}

final case class IndexEntry(file: String, pos: Int, length: Int, timestamp: Int)


