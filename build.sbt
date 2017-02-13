name := """hcube-scheduler"""

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "com.cronutils" % "cron-utils" % "5.0.5"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.8" % "test"

scalacOptions in Test ++= Seq("-Yrangepos")
