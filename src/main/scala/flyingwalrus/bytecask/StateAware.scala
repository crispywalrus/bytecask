package flyingwalrus.bytecask

import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean, AtomicReference}

object State extends Enumeration {
  type State = Value
  val Unmodified, AfterPut, AfterDelete, AfterPutAndDelete = Value
}

import flyingwalrus.bytecask.State._

trait StateAware {

  val bytecask: Bytecask

  val createdAt = System.currentTimeMillis()

  val lastAccessed = new AtomicLong(createdAt)

  val state = new AtomicReference[State](Unmodified)

  def getState = state.get

  def isDirty = state.get != Unmodified

  val active = new AtomicBoolean(true)

  def isActive = active.get

  def access[T](f: => T) = {
    if (!isActive) throw new IllegalAccessException("Bytecask is not active")
    lastAccessed.set(Utils.now)
    f
  }

  def resetState() = {
    state.set(Unmodified)
  }

  def putDone() = {
    val newState = state.get match {
      case Unmodified => AfterPut
      case AfterDelete => AfterPutAndDelete
      case _ => state.get
    }
    state.set(newState)
  }

  def deleteDone() = {
    val newState = state.get match {
      case Unmodified => AfterDelete
      case AfterPut => AfterPutAndDelete
      case _ => state.get
    }
    state.set(newState)
  }

}

