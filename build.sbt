organization := "net.crispywalrus.bytecask"

name := "bytecask"

licenses += ("ASL-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.11.8","2.12.0")

libraryDependencies ++= Seq(
  slf4j("api"),
  snappy,
  slf4j("simple") % Test,
  scalatest % Test
)

val compilerOptions = Seq(
  "-encoding", "utf8",
  "-feature",
  "-language:_",
  "-deprecation",
  "-Ywarn-unused-import",
  "-Ydelambdafy:method",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-target:jvm-1.8"
)

scalacOptions ++= compilerOptions ++ (
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2,p)) if p < 12 => Seq(
      "-optimise",
      "-Ybackend:GenBCode",
      "-Yopt:l:classpath")
    case _ => Seq(
      "-opt:l:classpath"
    )
  }
)

scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
}

scalacOptions in (Test, console) ~= {
  _.filterNot(Set("-Ywarn-unused-import"))
}

fork in run := true

javaOptions in run += "-Droot-level=OFF -XX:+TieredCompilation -XX:+AggressiveOpts -server -Xmx512M -Xss2M"

buildInfoKeys := Seq[BuildInfoKey](
  organization,
  name,
  version,
  scalaVersion,
  libraryDependencies in Compile,
  BuildInfoKey.action("gitVersion") {
    git.formattedShaVersion.?.value.
      getOrElse(Some("Unknown")).
      getOrElse("Unknown")+"@"+
    git.formattedDateVersion.?.value.getOrElse("")
  })
 
buildInfoPackage := "flyingwalrus.bytecask"

addCommandAlias("testCoverage","; clean; coverage; test; coverageReport")

def slf4j(name: String) = "org.slf4j" % s"slf4j-$name" % "1.7.21"
def snappy = "org.xerial.snappy" % "snappy-java" % "1.1.2.6"
def scalatest = "org.scalatest" %% "scalatest"  % "3.0.0"
