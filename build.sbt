name := """hcube-scheduler"""

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.0"
libraryDependencies += "com.cronutils" % "cron-utils" % "5.0.5"
libraryDependencies += "com.coreos" % "jetcd" % "0.1.0-SNAPSHOT"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.8" % "test"

scalacOptions in Test ++= Seq("-Yrangepos")
