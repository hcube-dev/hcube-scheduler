name := """hcube-scheduler"""
organization := "hcube"

version := "1.0"

scalaVersion := "2.11.8"

val DistConfig = config("DistConfig")
lazy val root = (project in file("."))
  .configs(DistConfig)
  .settings(inConfig(DistConfig)(Classpaths.ivyBaseSettings): _*)

resolvers += Resolver.mavenLocal

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.23"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.0"
libraryDependencies += "com.cronutils" % "cron-utils" % "5.0.5"
libraryDependencies += "com.coreos" % "jetcd" % "0.1.0-SNAPSHOT"

libraryDependencies += "com.typesafe" % "config" % "1.3.1" % "optional"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.1" % "optional"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.8" % "test"
libraryDependencies += "org.specs2" %% "specs2-mock" % "3.8.8" % "test"
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % "test"
libraryDependencies += "com.jayway.awaitility" % "awaitility-scala" % "1.7.0"

libraryDependencies in DistConfig := Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.1",
  "com.typesafe" % "config" % "1.3.1" % "optional"
)

// spec2 options
scalacOptions in Test ++= Seq("-Yrangepos")

lazy val copyLibs = taskKey[Unit]("copy libs to target/dist/lib")
copyLibs := {
  val log = streams.value.log
  val dest = target.value / "dist" / "lib"
  IO.createDirectory(dest)

  log.info(s"Copying target artifact to $dest")
  val targetFile = Keys.`package`.in(Compile).value
  IO.copyFile(targetFile, dest / targetFile.getName, preserveLastModified = true)

  log.info(s"Copying dependencies to $dest")
  val dependencies = (update in Compile).value.matching(configurationFilter(name = "compile"))
  IoUtils.copyIfChanged(PathFinder(dependencies), dest, msg => streams.value.log.info(msg))

  log.info(s"Copying dist dependencies to $dest")
  val distDependencies = (update in DistConfig).value.allFiles
  IoUtils.copyIfChanged(PathFinder(distDependencies), dest, msg => streams.value.log.info(msg))
}

lazy val copyFiles = taskKey[Unit]("copy scripts and configs to target/dist")
copyFiles := {
  val log = streams.value.log
  val scriptsDest = target.value / "dist" / "bin"
  val confDest = target.value / "dist" / "conf"

  log.info(s"Copying scripts to $scriptsDest")
  IoUtils.copyIfChanged(PathFinder(baseDirectory.value / "scripts" / "run.sh"),
    scriptsDest, msg => streams.value.log.info(msg))
  (scriptsDest / "run.sh").setExecutable(true)

  log.info(s"Copying confs to $confDest")
  IoUtils.copyIfChanged(PathFinder(baseDirectory.value / "conf" / "logback.xml"),
    confDest, msg => log.info(msg))
}

lazy val dist = taskKey[Unit]("prepare distributable directory")
dist := {}
dist <<= dist dependsOn copyLibs
dist <<= dist dependsOn copyFiles
