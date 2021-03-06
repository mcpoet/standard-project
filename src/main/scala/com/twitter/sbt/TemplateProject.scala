package com.twitter.sbt

// from http://github.com/debasishg/sjson/blob/master/project/build/TemplateProject.scala

import _root_.sbt._
import FileUtilities.{clean, createDirectory}

trait TemplateProject extends DefaultProject with FileTasks
{
  // declares fmpp as a managed dependency.  By declaring it in the private 'fmpp' configuration, it doesn't get published
  val fmppDep = "net.sourceforge.fmpp" % "fmpp" % "0.9.13" % "fmpp"
  val fmppConf = config("fmpp") hide
  def fmppClasspath = configurationClasspath(fmppConf)

  // declare the directory structure for the processed sources
  def srcManaged: Path = "src_managed"
  override def mainScalaSourcePath = srcManaged / "main"
  override def testScalaSourcePath = srcManaged / "test"

  // declare the directory structure for the templates
  def srcRoot: Path = "src" / "main" / "scala"
  def testSrcRoot: Path = "src" / "test" / "scala"

  // global variables available within templates
  def fmppTemplateData = {
    val versionListWithDefaults = buildScalaVersion.split("\\.", 4).toList ::: List("_")
    val vMajor :: vMinor :: vPatch :: vExtra :: _ = versionListWithDefaults
    Map("scalaVersion"      -> (vMajor +"."+ vMinor),
        "fullScalaVersion"  -> buildScalaVersion,
        "scalaVersionMajor" -> vMajor,
        "scalaVersionMinor" -> vMinor,
        "scalaVersionPatch" -> vPatch,
        "scalaVersionExtra" -> vExtra)
  }
  def fmppTemplateDataString = fmppTemplateData.map { case (k,v) => k +": "+ v }.mkString(", ")

  // arguments to fmpp
  def fmppArgs = "--ignore-temporary-files" :: "-D" :: fmppTemplateDataString :: Nil

  // creates a task that invokes fmpp
  def fmppTask(args: => List[String], output: => Path, srcRoot: => Path, sources: PathFinder) = {
    runTask(Some("fmpp.tools.CommandLine"), fmppClasspath,
      "-U" :: "all" :: "-S" :: srcRoot.absolutePath :: "-O" :: output.absolutePath :: args ::: sources.getPaths.toList)
  }

  //  Define template actions and make them run before compilation

  lazy val template = fmppTask(fmppArgs, mainScalaSourcePath, srcRoot, sources(srcRoot))
  lazy val testTemplate = fmppTask(fmppArgs, testScalaSourcePath, testSrcRoot, sources(testSrcRoot))

  override def compileAction = super.compileAction dependsOn(template)
  override def testCompileAction = super.testCompileAction dependsOn(testTemplate)
}
