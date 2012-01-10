package bytecask

/*
* Copyright 2011 P.Budzik
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* User: przemek
* Date: 7/2/11
* Time: 12:07 PM
*/

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import bytecask.Utils._
import bytecask.Bytes._

class ConcurrentSuite extends FunSuite
with ShouldMatchers with BeforeAndAfterEach with TestSupport {

  var db: Bytecask = _

  test("put/get") {
    val threads = 1000
    val iters = 100
    concurrently(threads, iters) {
      i => db.put(i.toString, randomBytes(1024))
    }
    concurrently(threads, iters) {
      i => assert(!db.get(i.toString).isEmpty)
    }
  }

  test("put/delete") {
    val threads = 1000
    concurrently(threads) {
      i => db.put(i.toString, randomBytes(1024))
    }
    concurrently(threads) {
      i => db.delete(i.toString)
    }
    db.count() should be(0)
  }

  override def beforeEach() {
    db = new Bytecask(mkTmpDir.getAbsolutePath)
  }

  override def afterEach() {
    db.destroy()
  }
}