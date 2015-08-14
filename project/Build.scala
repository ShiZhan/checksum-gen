import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object MyBuild extends Build {
  val scalaV = Option(System.getProperty("scala.version")).getOrElse("2.11.6")
  val codecV = "1.10"
  val comprV = "1.9"

  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    version      := "0.1-SNAPSHOT",
    organization := "com.simba",
    scalaVersion := scalaV,
    scalacOptions in Compile ++= Seq(
      "-encoding", "UTF-8",
      "-deprecation", "-feature", "-unchecked",
      "-Xlint"),
    libraryDependencies ++= Seq(
      "commons-codec"      % "commons-codec"    % codecV,
      "org.apache.commons" % "commons-compress" % comprV
    )
  )

  lazy val buildSettings = commonSettings

  lazy val ChecksumGen = Project(
    id = "ChecksumGen",
    base = file("."),
    settings = commonSettings ++
    sbtassembly.Plugin.assemblySettings ++
    Seq(
      logLevel in assembly := Level.Warn
    )
  )
}
