package flyingwalrus.bytecask

import org.scalatest._
import Utils._
import Bytes._
import Files._

class ConcurrentSuite extends FunSuite with Matchers with BeforeAndAfterEach with TestSupport {

  var db: Bytecask = _

  test("put/get 1K") {
    val threads = 64
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i, randomBytes(1024))
    }
    concurrently(threads, iters) {
      i => assert(!db.get(i).isEmpty, "not empty")
    }
    db.count() should be(iters)
  }

  test("put/get 32K") {
    val threads = 64
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i, randomBytes(1024 * 32))
    }
    concurrently(threads, iters) {
      i => assert(!db.get(i).isEmpty, "not empty")
    }
    db.count() should be(iters)
  }

  test("put/delete") {
    val threads = 64
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i, randomBytes(1024))
    }
    concurrently(threads, iters) {
      i => db.delete(i)
    }
    db.count() should be(0)
  }

  test("mixed put/get") {
    val threads = 64
    val iters = 1000
    db.put("a", randomBytes(1024 * 64))
    concurrently(threads, iters) {
      i => db.put(randomBytes(256), randomBytes(1024))
      assert(!db.get("a").isEmpty, "not empty")
    }
    db.count() should be(iters + 1)
  }

  override def beforeEach() = db = new Bytecask(mkTempDir)

  override def afterEach() = db.destroy()

}
