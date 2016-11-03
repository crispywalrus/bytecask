package flyingwalrus.bytecask

import management.ManagementFactory
import javax.management.ObjectName

trait JmxSupport {

  val name: String

  val bytecask: Bytecask

  val beanName = new ObjectName("Bytecask_" + name + ":type=BytecaskBean")

  lazy val server = ManagementFactory.getPlatformMBeanServer

  def jmxInit() = {
    server.registerMBean(new BytecaskJmx(bytecask), beanName)
  }

  def jmxDestroy() = {
    server.unregisterMBean(beanName)
  }
}

trait BytecaskJmxMBean {
  def stats(): String
}

class BytecaskJmx(db: Bytecask) extends BytecaskJmxMBean {
  def stats() = db.stats()
}

