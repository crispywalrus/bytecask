package flyingwalrus.bytecask

import java.util.concurrent.{TimeUnit, Executors}

trait TestSupport {

  def concurrently(threads: Int, iters: Int = 1, timeout: Int = 5)(f: Int => Any) = {
    val pool = Executors.newFixedThreadPool(threads)
    for (i <- 1.to(iters))
      pool.execute(new Runnable() {
        override def run(): Unit = f(i)
      })
    pool.shutdown()
    pool.awaitTermination(timeout.toLong, TimeUnit.SECONDS)
  }

}
