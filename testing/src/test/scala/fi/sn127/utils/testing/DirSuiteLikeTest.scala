/*
 * Copyright 2016-2017 Jani Averbach
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
 */
package fi.sn127.utils.testing

import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystems, Files}

import org.scalatest.events.{Event, TestFailed}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Args, FlatSpec, Inside, Matchers, Reporter}

import fi.sn127.utils.fs.{FileUtils, Glob, Regex}

/**
 * How to test the Testers?
 *
 *    Hi there - Scalatest my dear friend!
 *
 * Examples and "How to" for these tests has been
 * taken from Scalatest's own test suite, especially from
 * FunSuiteSuite.scala
 *
 * https://github.com/scalatest/scalatest/tree/3.0.x/
 * scalatest-test/src/test/scala/org/scalatest/FunSuiteSuite.scala
 */
@SuppressWarnings(Array(
  "org.wartremover.warts.Var",
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
class YeOldeDirSuiteSpec extends FlatSpec with Matchers with Inside {

  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/dirsuite").toAbsolutePath.normalize
  val fu = FileUtils(filesystem)

  object DummyProg {
    // negative values so that these won't mix up
    // with arg list lengths
    val SUCCESS = -1
    val FAILURE = -2

    def mainSuccess(args: Array[String]): Int = {
      SUCCESS
    }

    def mainFail(args: Array[String]): Int = {
      FAILURE
    }

    def mainArgsCount(args: Array[String]): Int = {
      args.length
    }

    def mainTxt(args: Array[String]): Int = {
      val output = fu.getPath(testdir.toString, args(0))
      Files.write(output, args
          .mkString("hello\n", "\n", "\nworld\n")
          .getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainXml(args: Array[String]): Int = {
      val output = fu.getPath(testdir.toString, args(0))
      Files.write(output, args
        .mkString("<hello><arg>", "</arg><arg>", "</arg></hello>\n")
        .getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainTxtXml(args: Array[String]): Int = {
      val result =
        if (args(1) === "txt") {
          mainTxt(args)
        } else if (args(1) === "xml") {
          mainXml(args)
        } else {
          FAILURE
        }
      result
    }
  }

  class LifeIsGoodReporter extends Reporter {
    var lifeIsGood = true
    def apply(event: Event): Unit = {
      event match {
        case event: TestFailed =>
          lifeIsGood = false
        case _ =>
      }
    }
  }

  trait FailureReporter extends Reporter {
    var lifeIsGood = false

    def isThisGood(ex: Throwable, msg: String): Boolean

    def apply(event: Event): Unit = {
      event match {
        case event: TestFailed =>
          lifeIsGood = false
          event.throwable match {
            case Some(ex) =>
              lifeIsGood = isThisGood(ex, event.message)
            case _ =>
          }
        case _ =>
      }
    }
  }

  class TestVectorFailureReporter extends FailureReporter {
    def isThisGood(ex: Throwable, msg: String): Boolean = {
      ex match {
        case tvex: TestVectorException =>
          msg.startsWith(DirSuiteLike.testVectorFailureMsgPrefix)
        case _ =>
          false
      }
    }
  }


  class TestVectorExceptionReporter extends FailureReporter {
    def isThisGood(ex: Throwable, msg: String): Boolean = {
      ex match {
        case tvex: TestVectorException =>
          msg.startsWith(DirSuiteLike.testVectorExceptionMsgPrefix)
        case _ =>
          false
      }
    }
  }

  class ExecutionExceptionReporter extends FailureReporter {
    def isThisGood(ex: Throwable, msg: String): Boolean = {
      ex match {
        case tfe: TestFailedException =>
          msg.startsWith(DirSuiteLike.executionFailureMsgPrefix)
        case _ =>
          false
      }
    }
  }

  class AssertionErrorReporter extends FailureReporter {
    def isThisGood(ex: Throwable, msg: String): Boolean = {
      ex match {
        case tfe: java.lang.AssertionError =>
          msg.startsWith("assertion failed")
        case _ =>
          false
      }
    }
  }

  behavior of "ignoreDirSuite"
  it must "ignore files" in {
    // TODO: Check somehow how many files/tests this actually ignored
    var runCount = 0
    class DirSuite extends DirSuiteLike {
      ignoreDirSuite(testdir, Regex("success/tr[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(4) {
          runCount = runCount + 1
          DummyProg.mainArgsCount(args)
        }
      }
    }
    val t = new DirSuite
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 0)
  }

  behavior of "runDirSuite"
  it must "work with empty exec file (e.g. only rows of plain ';'s)" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/noargs[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(0) {
          runCount = runCount + 1
          DummyProg.mainArgsCount(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 2)
  }
  it must "work with glob patterns" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Glob("success/noargs*.exec")) { args: Array[String] =>
        assertResult(0) {
          runCount = runCount + 1
          DummyProg.mainArgsCount(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 2)
  }
  it must "work with regex patterns" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/noargs.*\\.exec")) { args: Array[String] =>
        assertResult(0) {
          runCount = runCount + 1
          DummyProg.mainArgsCount(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 2)
  }

  it must "work without output files" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/tr[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(4) {
          runCount = runCount + 1
          DummyProg.mainArgsCount(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 3)
  }
  it must "work with valid txt-output files" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/txt[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          runCount = runCount + 1
          DummyProg.mainTxt(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    // txt01 => 1 exec row
    // txt02 => 1 exec row
    // txt03 => 3 exec rows
    assert(runCount === 5)
  }

  it must "work with valid xml-output files" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/xml[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          runCount = runCount + 1
          DummyProg.mainXml(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 1)
  }

  it must "work with valid xml and txt -output files at the same time" in {
    var runCount = 0
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/txtxml[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          runCount = runCount + 1
          DummyProg.mainTxtXml(args)
        }
      }
    }
    val t = new TestRunner
    val r = new LifeIsGoodReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)

    assert(runCount === 2)
  }

  it must "detect plain asserts" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/tr[0-9]+\\.exec")) { args: Array[String] =>
         scala.Predef.assert(DummyProg.SUCCESS == DummyProg.mainFail(args))
      }
    }
    val t = new TestRunner
    val r = new AssertionErrorReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  it must "detect plain execution errors with assertResult" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/tr[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainFail(args)
        }
      }
    }
    val t = new TestRunner
    val r = new ExecutionExceptionReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  it must "detect missing dirsuite-tree" in {
    val ex = intercept[DirSuiteException] {
      class TestRunner extends DirSuiteLike {
        val not_there = fu.getPath(testdir.toString, "dirsuite-tree-is-not-there")
        runDirSuite(not_there, Regex("failure/missing[0-9]+\\.exec")) { args: Array[String] =>
          assertResult(DummyProg.SUCCESS) {
            DummyProg.mainSuccess(args)
          }
        }
      }
      val t = new TestRunner
      val r = new ExecutionExceptionReporter
      t.run(None, Args(r))
    }
    assert(ex.getMessage.startsWith("=>"))
  }

  it must "detect missing output files" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/missing[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainSuccess(args)
        }
      }
    }
    val t = new TestRunner
    val r = new TestVectorExceptionReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  it must "detect erroneous txt output" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/txt01\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainTxt(args)
        }
      }
    }
    val t = new TestRunner
    val r = new TestVectorFailureReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  ignore should "detect erroneous end-of-line txt-output" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("success/txt02\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainTxt(args)
        }
      }
    }
    val t = new TestRunner
    //val r = new LifeIsGoodReporter
    val r = new TestVectorFailureReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  it must "detect erroneous xml output" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/xml[0-9]+\\.exec")) { args: Array[String] =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainXml(args)
        }
      }
    }
    val t = new TestRunner
    val r = new TestVectorFailureReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }

  it must "detect and report validator exceptions (XML SAX, etc)" in {
    class TestRunner extends DirSuiteLike {
      runDirSuite(testdir, Regex("failure/xml-sax[0-9]+\\.exec")) { (args: Array[String]) =>
        assertResult(DummyProg.SUCCESS) {
          DummyProg.mainXml(args)
        }
      }
    }
    val t = new TestRunner
    val r = new TestVectorExceptionReporter
    t.run(None, Args(r))
    assert(r.lifeIsGood)
  }
}
