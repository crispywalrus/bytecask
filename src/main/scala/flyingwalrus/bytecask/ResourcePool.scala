package flyingwalrus.bytecask

import java.util.concurrent.{ConcurrentHashMap, TimeUnit, ConcurrentLinkedQueue, Semaphore}
import java.io.RandomAccessFile
import scala.collection.JavaConversions._
import scala.reflect.{ClassTag, classTag}
import scala.util.{Try,Success,Failure}

/**
 * Instead of creating a RandomAccessFile per each reading thread the idea
 * is to keep a pool of readers per each file and reuse it keeping it open.
 * This avoids costly file handlers creation for the same files.
 */
abstract class ResourcePool[T <: {def close(): Unit} : ClassTag](maxResources: Int) {

  private final val semaphore = new Semaphore(maxResources, true)

  private final val resources = new ConcurrentLinkedQueue[T]()

  def createResource(): T

  def get(maxWaitMillis: Long): T = {
    semaphore.tryAcquire(maxWaitMillis, TimeUnit.MILLISECONDS)
    resources.poll() match {
      case r if classTag[T].runtimeClass.isInstance(r) => r
      case _ =>
        val create = Try { createResource() }
        semaphore.release()
        create match {
          case Failure(ex) => throw new RuntimeException("Cannot create resource", ex)
          case Success(t) => t
        }
    }
  }

  def release(resource: T): Unit = {
    resources.add(resource)
    semaphore.release()
  }

  def destroy(): Unit = {
    resources.foreach(_.close())
  }
}

abstract class FileReadersPool[T <: {def close(): Unit} : ClassTag](maxReaders: Int) {

  private val cache = new ConcurrentHashMap[String, ResourcePool[T]]()

  def get(file: String): T = {
    val pool = cache.get(file) match {
      case pool: ResourcePool[T] => pool
      case _ => {
        val pool = new ResourcePool[T](maxReaders) {
          def createResource() = createReader(file)
        }
        cache.put(file, pool)
        pool
      }
    }
    pool.get(1000)
  }

  def release(file: String, reader: T): Unit = {
    cache.get(file).release(reader)
  }

  def createReader(file: String): T

  def invalidate(file: String): Unit = {
    cache.get(file) match {
      case pool: ResourcePool[T] => pool.destroy()
      case _ => cache.remove(file)
    }
  }

  def destroy(): Unit = {
    cache.foreach(_._2.destroy())
  }
}

/**
 * TODO: if there happens to be many files open, the file OS limit may be exceeded
 * Might be a LRU cache w/ open files closure
 */

class RandomAccessFilePool(maxFiles: Int) extends FileReadersPool[RandomAccessFile](maxFiles) {
  def createReader(file: String) = new RandomAccessFile(file, "r")

  override def get(file: String) = super.get(file) match {
    case reader if !reader.getChannel.isOpen =>
      invalidate(file)
      get(file)
    case reader => reader
  }
}
