package com.intenthq.pucket.avro

import com.intenthq.pucket.avro.test.{AvroTest, AvroTest2}
import org.scalacheck.{Gen, Prop}
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.{ScalaCheck, Specification}


class AvroPucketDescriptorSpec extends Specification with DisjunctionMatchers with ScalaCheck {
  import AvroPucketDescriptorSpec._
  import AvroTestUtils._

  def is =
    s2"""
       The Pucket Descriptor
         Can serialise and deserialise from JSON $fromJson
         Fails to deserialise a non-json string $badString
         Fails when avro schema isn't valid $badSchema
         Fails when schema is not for the expected type $schemaClassIncorrect
      """

  def fromJson =
    Prop.forAll(descriptorGen) { d =>
      AvroPucketDescriptor[AvroTest](AvroTest.getClassSchema, d.toString) must be_\/-.like {
        case a =>
          (a.schema === d.schema) and
          (a.compression === d.compression) and
          (a.partitioner.map(_.getClass) === d.partitioner.map(_.getClass))
      }
    }
  
  def badString =
    Prop.forAll(randomString) { s =>
      AvroPucketDescriptor[AvroTest](AvroTest.getClassSchema, s) must be_-\/[Throwable]
    }

  def badSchema =
    AvroPucketDescriptor[AvroTest](AvroTest.getClassSchema, badSchemaString) must be_-\/[Throwable]

  def schemaClassIncorrect =
    AvroPucketDescriptor[AvroTest](AvroTest.getClassSchema, incorrectSchema) must be_-\/.like {
      case a => a must beAnInstanceOf[RuntimeException]
    }
}

object AvroPucketDescriptorSpec {
  val rng = scala.util.Random
  val badSchemaString = json("Nic Cage is the best actor of all time")
  val incorrectSchema = json(AvroTest2.getClassSchema.toString.replace(""""""", """\""""))
  def json(clazz: String) = s"""{"avroSchema": "$clazz", "compression": "SNAPPY"}"""
  def randomString: Gen[String] = rng.nextString(50)
}