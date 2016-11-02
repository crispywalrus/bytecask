organization := "net.crispywalrus.bytecask"

name := "bytecask"

licenses += ("ASL-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVer,
  "org.xerial.snappy" % "snappy-java" % "1.1.2.6",
  "org.slf4j" % "slf4j-simple" % slf4jVer % Test,
  "org.scalatest" %% "scalatest"  % "3.0.0"  % Test
)

val slf4jVer = "1.7.21"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-feature",
  "-language:_",
  "-deprecation",
  "-optimise",
  "-Yinline-warnings",
  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-target:jvm-1.8",
  "-Yopt:l:classpath"
)

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

addCommandAlias("testCoverage","; clean; coverage; test")
