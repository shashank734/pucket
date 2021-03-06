package com.intenthq.pucket.writer

import com.intenthq.pucket.{TestLogging, TestUtils}
import TestUtils._
import org.specs2.Specification
import org.specs2.scalaz.DisjunctionMatchers

import scalaz.\/

trait IncrementalWriterSpec[T] extends Specification with DisjunctionMatchers with TestLogging {
  import IncrementalWriterSpec._

  val wrapper: PucketWrapper[T]
  def newData(i: Long): T

  val data = 1.to(500).map(_ => newData(rng.nextLong()))
  val expectedFiles = data.length / maxWrites

  def is =
    s2"""
        Writes the expected number of files $writeTest
        Removes the temp dir ${step(wrapper.close())}
      """

  def writeAndVerify = writeTest and verifyFiles

  def writeTest =
    writer.flatMap(writeData(data, _)) must beRightDisjunction.like {
      case a => a.close must be_\/-
    }

  def verifyFiles =
    numberOfFiles must beRightDisjunction.like {
      case a => a === expectedFiles
    }

  def numberOfFiles: Throwable \/ Int =
    wrapper.pucket.flatMap(p => wrapper.pucket.map(x => p.fs.listStatus(x.path)
      .count(_.getPath.getName.contains(".parquet"))))

  def writer: (Long, Throwable) \/ IncrementalWriter[T] =
    wrapper.pucket.leftMap((0L, _)).flatMap(IncrementalWriter(0, _, maxWrites))

}

object IncrementalWriterSpec {
  val rng = scala.util.Random
  val maxWrites = 50
}
