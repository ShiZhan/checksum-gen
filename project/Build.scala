import sbt._
import Keys._

object MyBuild extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.1-SNAPSHOT",
    organization := "com.simba",
    scalaVersion := "2.11.6"
  )

  lazy val ChecksumGen = Project(
    id = "ChecksumGen",
    base = file("."),
    settings = Defaults.defaultSettings ++
    sbtassembly.Plugin.assemblySettings
  )
}
