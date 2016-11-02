#### Bytecask - low latency k/v storage component ####


[ ![Codeship Status for crispywalrus/bytecask](https://codeship.com/projects/466089b0-de6c-0133-a2ed-4612197ec823/status)](https://codeship.com/projects/144753)
[![codecov](https://codecov.io/gh/crispywalrus/bytecask/branch/master/graph/badge.svg)](https://codecov.io/gh/crispywalrus/bytecask)

* lightweight - no dependencies, no underlying storages
* embeddable building block, no daemons, no external scripts needed
* storage component for distributed NoSQL databases
* inspired by [Bitcask](https://github.com/basho/bitcask) (Erlang)
* optional fast compression (backed by [snappy-java](http://code.google.com/p/snappy-java/))
* support for expiration and eviction
* support for keys with common prefixes (i.e. urls, files etc)
* blob store/retrieve (i.e. for files)
* passivation/activation (if db is idle we may release resources and restore it when necessary)
* Apache 2.0 License

### Key properties: ###

* keys in memory
* low latency write (appending)
* low latency read (direct read)
* values can be blobs (large values)
* compaction/merging

### Install ###

**sbt**

````libraryDependencies += "net.crispywalrus.bytecask" %% "bytecask" % $VERSION````

### Example ###

```scala

val db = new Bytecask("/home/foo")
db.put("foo", "some value...")
println(db.get("foo"))
db.delete("foo")
db.destroy()

//other methods of initialization:

val db = new Bytecask("/home/foo") with Compression
val db = new Bytecask("/home/foo", prefixedKeys=true) with Compression
val db = new Bytecask("/home/foo") with Eviction { val maxCount = 3 }
val db = new Bytecask("/home/foo") with Expiration { val ttl = 180 }
val db = new Bytecask("/home/foo") with BlobStore { val blockSize = 1024 * 1024 }
val db = new Bytecask("/home/foo") with Compression with Expiration with BlobStore {
    val ttl = 180
    val blockSize = 1024 * 1024
}
val db = new Bytecask("/home/foo") with JmxSupport

...

//blob store example

val db = new Bytecask(mkTempDir) with BlobStore {
      val blockSize = 1024 * 1024
}

// store a blob (i.e. file) passing an input stream to read data from

db.storeBlob(name, new FileInputStream(...))

// retrieve the blob passing an output stream to write the blob to

withResource(new FileOutputStream(...)) {
      os => db.retrieveBlob(name, os)
}

...

// passivation/activation example

if (db.idleTime > 15*60*1000)
    db.passivate()

// passivation releases memory occupied by the index (data is not gone though)
// now all access methods throw exception as the db is inactive (no references are kept)

if (!db.isActive)
    db.activate()

// activation restores index from files and sets 'active' state

```

More -> [See the tests](https://github.com/pbudzik/bytecask/blob/master/src/test/scala/com/github/bytecask/BasicSuite.scala)

### API ###
```scala

  def put(key: Array[Byte], value: Array[Byte])

  def get(key: Array[Byte]): Option[Array[Byte]]

  def delete(key: Array[Byte]): Option[Array[Byte]]

  def keys(): Set[Array[Byte]]

  def values(): Iterator[Array[Byte]]

  def merge(): Unit

  def close(): Unit

  def destroy(): Unit

  def passivate(): Unit

  def activate(): Unit

  def idleTime: Long

  def isActive: Boolean

  def isDirty: Boolean

```
### Glossary ###

* Index (Keydir) - keys are kept in memory and point to entries in data files
* Data files - files that contain the actual data
* Merge - depending on update/delete intensity more and more space is occupied, so
merge operation compacts data as well as merges files into one
* Hint files - when files are merged to one file a "hint file" is produced out of the new file being
a persisted index, so later the index can be rebuilt w/o processing the data file (however anyway
can be built)
* Prefixed keys - keys that contain common prefixes like file paths, URLs etc. In this case it is not efficient
to allocate memory for all those repetitive byte sequences. It is advisable to turn prefixed keys mode on, in order
to have a dedicated map implementation being used (based on [Patricia Trie](http://en.wikipedia.org/wiki/Radix_tree))
that keeps data in a tree manner so that common parts of keys are reused vs allocated separately
* Passivation - if we maintain multiple Bytecask instances (say per user) and some of them are not being used frequently
it may not be critical to keep all indexes in memory. Passivation puts an instance "on hold",to be activated later,
what means the index will have to be reread to memory later. This may improve overall resources management/scalabilty
at the price of occasional activation time.
* Blob store - internal architecture has inherent limitation as to the value size as it is internally represented as
an array of bytes. It means that blobs (files included) cannot be easily stored. The blob store function of the API
breaks blob's value down to segments so that multiple segments (plus a descriptor entry) altogether hold the value.
Storing and retrieving relies on streams rather than on byte arrays as the value by definition is large.
* Eviction/Expiration - eviction is a mechanism to manage which entries should be removed (at the moment based on max items),
expiration is removal based on time (TTL).

### Benchmark ####

```
--- Benchmark 1 - small values...

+ db: 482bzKWs, /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109084429_1

*** sequential put of different 10000 items: time: 142 ms, 1 op: 0.0142 ms, throughput: 70422 TPS at 8.60 MB/s, total: 1.22 MB
*** sequential get of different 10000 items: time: 90 ms, 1 op: 0.009 ms, throughput: 111111 TPS at 13.56 MB/s, total: 1.22 MB
*** sequential get of random items 10000 times: time: 83 ms, 1 op: 0.0083 ms, throughput: 120481 TPS at 14.71 MB/s, total: 1.22 MB
*** sequential get of the same item 10000 times: time: 63 ms, 1 op: 0.0063 ms, throughput: 158730 TPS at 19.38 MB/s, total: 1.22 MB

*** concurrent put of different 10000 items: time: 239 ms, 1 op: 0.0239 ms, throughput: 41841 TPS at 5.11 MB/s, total: 1.22 MB
*** concurrent get of the same item 10000 times: time: 46 ms, 1 op: 0.0046 ms, throughput: 217391 TPS at 26.54 MB/s, total: 1.22 MB
*** concurrent get of random 10000 items: time: 45 ms, 1 op: 0.0045 ms, throughput: 222222 TPS at 27.13 MB/s, total: 1.22 MB
name: 482bzKWs, dir: /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109084429_1, uptime: 942, count: 11000, splits: 0, merges: 0

--- Benchmark 2 - big values...

+ db: kQc51WhD, /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109085511_2

*** sequential put of different 10000 items: time: 965 ms, 1 op: 0.0965 ms, throughput: 10362 TPS at 647.67 MB/s, total: 625.00 MB
*** sequential get of different 10000 items: time: 512 ms, 1 op: 0.0512 ms, throughput: 19531 TPS at 1220.70 MB/s, total: 625.00 MB
*** sequential get of random items 10000 times: time: 427 ms, 1 op: 0.0427 ms, throughput: 23419 TPS at 1463.70 MB/s, total: 625.00 MB
*** sequential get of the same item 10000 times: time: 489 ms, 1 op: 0.0489 ms, throughput: 20449 TPS at 1278.12 MB/s, total: 625.00 MB

*** concurrent put of different 10000 items: time: 1169 ms, 1 op: 0.1169 ms, throughput: 8554 TPS at 534.64 MB/s, total: 625.00 MB
*** concurrent get of the same item 10000 times: time: 311 ms, 1 op: 0.0311 ms, throughput: 32154 TPS at 2009.65 MB/s, total: 625.00 MB
*** concurrent get of random 10000 items: time: 356 ms, 1 op: 0.0356 ms, throughput: 28089 TPS at 1755.62 MB/s, total: 625.00 MB
name: kQc51WhD, dir: /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109085511_2, uptime: 4403, count: 11000, splits: 0, merges: 0

--- Benchmark 3 (splitting)...

*** sequential put of different 100 items w/ frequent splits: time: 37 ms, 1 op: 0.37 ms, throughput: 2702 TPS at 5.28 MB/s, total: 0.20 MB
Fetching entries... 
Files in db's directory: [0,1,10,2,3,4,5,6,7,8,9]
name: sdYx3x8e, dir: /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109089921_3, uptime: 63, count: 100, splits: 10, merges: 0

--- Benchmark 3 (merging)...

*** sequential put of different 100 items w/ frequent splits: time: 44 ms, 1 op: 0.44 ms, throughput: 2272 TPS at 4.44 MB/s, total: 0.20 MB
sequential delete of random 20 items
sequential over-put of random 20 items
Files in db's directory: [0,1,10,11,12,13,14,15,16,17,18,19,2,20,3,4,5,6,7,8,9]
Directory size: 209547 bytes
Directory size: 159203 bytes
Reduction: 209547 | 159203 -> 24.03%
name: ToTEaCHt, dir: /var/folders/sz/npzmr0lj5xb4s6464hpfnwtx7ypvyl/T/_1478109089992_4, uptime: 188, count: 80, splits: 20, merges: 1
```

older

```
Date: 5/4/2013
Hardware: Intel Core 2 Quad CPU Q6600@2.40GHz, SSD disk
OS: Ubuntu 12.10, 3.2.0-41-generic x86_64
Java: 1.7.0_15-b03 64-bit with -server -XX:+TieredCompilation -XX:+AggressiveOpts

--- Benchmark 1 - small values...

+ db: R6W1OOTp, /tmp/_1367674596629_1

*** sequential put of different 10000 items: time: 199 ms, 1 op: 0.0199 ms, throughput: 50251 TPS at 6.13 MB/s, total: 1.22 MB
*** sequential get of different 10000 items: time: 161 ms, 1 op: 0.0161 ms, throughput: 62111 TPS at 7.58 MB/s, total: 1.22 MB
*** sequential get of random items 10000 times: time: 110 ms, 1 op: 0.011 ms, throughput: 90909 TPS at 11.10 MB/s, total: 1.22 MB
*** sequential get of the same item 10000 times: time: 84 ms, 1 op: 0.0084 ms, throughput: 119047 TPS at 14.53 MB/s, total: 1.22 MB

*** concurrent put of different 10000 items: time: 301 ms, 1 op: 0.0301 ms, throughput: 33222 TPS at 4.06 MB/s, total: 1.22 MB
*** concurrent get of the same item 10000 times: time: 109 ms, 1 op: 0.0109 ms, throughput: 91743 TPS at 11.20 MB/s, total: 1.22 MB
*** concurrent get of random 10000 items: time: 86 ms, 1 op: 0.0086 ms, throughput: 116279 TPS at 14.19 MB/s, total: 1.22 MB
name: R6W1OOTp, dir: /tmp/_1367674596629_1, uptime: 1674, count: 11000, splits: 0, merges: 0

--- Benchmark 2 - big values...

+ db: 3xR3ncaV, /tmp/_1367674598530_2

*** sequential put of different 10000 items: time: 5609 ms, 1 op: 0.5609 ms, throughput: 1782 TPS at 111.43 MB/s, total: 625.00 MB
*** sequential get of different 10000 items: time: 13056 ms, 1 op: 1.3056 ms, throughput: 765 TPS at 47.87 MB/s, total: 625.00 MB
*** sequential get of random items 10000 times: time: 8231 ms, 1 op: 0.8231 ms, throughput: 1214 TPS at 75.93 MB/s, total: 625.00 MB
*** sequential get of the same item 10000 times: time: 1882 ms, 1 op: 0.1882 ms, throughput: 5313 TPS at 332.09 MB/s, total: 625.00 MB

*** concurrent put of different 10000 items: time: 11438 ms, 1 op: 1.1438 ms, throughput: 874 TPS at 54.64 MB/s, total: 625.00 MB
*** concurrent get of the same item 10000 times: time: 897 ms, 1 op: 0.0897 ms, throughput: 11148 TPS at 696.77 MB/s, total: 625.00 MB
*** concurrent get of random 10000 items: time: 8525 ms, 1 op: 0.8525 ms, throughput: 1173 TPS at 73.31 MB/s, total: 625.00 MB
name: 3xR3ncaV, dir: /tmp/_1367674598530_2, uptime: 49775, count: 11000, splits: 0, merges: 0

```

You can build a jar in sbt:

    > assembly

and issue:

    bin/benchmark.sh

or from within sbt:

    run-main flyingwalrus.bytecask.Benchmark

### Collaboration ###

**Reporting bugs and asking for features**

You can use github issue tracker to report bugs or to ask for new features [here](https://github.com/pbudzik/bytecask/issues)

**Submit patches**

Patches are gladly welcome from their original author. Along with any
patches, please state that the patch is your original work and that
you license the work to the Bytecask project under the ASL 2.0 license

To propose a patch, fork the project and send a pull request via github.

**Any feedback, ideas, suggestions, success stories etc are more than welcome!**
