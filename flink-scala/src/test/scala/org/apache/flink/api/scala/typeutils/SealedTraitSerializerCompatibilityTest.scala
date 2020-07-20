package org.apache.flink.api.scala.typeutils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.util.TestLogger
import org.junit.Test
import org.scalatest.junit.JUnitSuiteLike
import org.apache.flink.api.scala._
import org.apache.flink.api.scala.typeutils.SealedTraitSerializerCompatibilityTest._
import org.apache.flink.core.memory.{DataInputViewStreamWrapper, DataOutputViewStreamWrapper}

class SealedTraitSerializerCompatibilityTest extends TestLogger with JUnitSuiteLike {

  @Test
  def testSameSubtypes(): Unit = {
    val config = new ExecutionConfig()
    val a = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[AOne], classOf[ATwo]),
      subtypeSerializers = Array(
        createTypeInformation[AOne].createSerializer(config),
        createTypeInformation[ATwo].createSerializer(config)))
    assert(compatibility(a, a).isCompatibleAsIs)
  }

  @Test
  def testSubtypesReordered(): Unit = {
    val config = new ExecutionConfig()
    val a = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[AOne], classOf[ATwo]),
      subtypeSerializers = Array(
        createTypeInformation[AOne].createSerializer(config),
        createTypeInformation[ATwo].createSerializer(config)))

    val b = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[ATwo], classOf[AOne]),
      subtypeSerializers = Array(
        createTypeInformation[ATwo].createSerializer(config),
        createTypeInformation[AOne].createSerializer(config)
        ))
    assert(compatibility(a, b).isIncompatible)
  }

  @Test
  def testAddSubtype(): Unit = {
    val config = new ExecutionConfig()
    val a = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[AOne], classOf[ATwo]),
      subtypeSerializers = Array(
        createTypeInformation[AOne].createSerializer(config),
        createTypeInformation[ATwo].createSerializer(config)))

    val b = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[AOne], classOf[ATwo], classOf[AThree]),
      subtypeSerializers = Array(
        createTypeInformation[AOne].createSerializer(config),
        createTypeInformation[ATwo].createSerializer(config),
        createTypeInformation[AThree].createSerializer(config)
      ))
    assert(compatibility(a, b).isIncompatible)
  }

  @Test
  def testRemoveSubtype(): Unit = {
    val config = new ExecutionConfig()
    val a = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[AOne], classOf[ATwo]),
      subtypeSerializers = Array(
        createTypeInformation[AOne].createSerializer(config),
        createTypeInformation[ATwo].createSerializer(config)))

    val b = new SealedTraitSerializer[ADT](
      subtypeClasses = Array(classOf[ATwo]),
      subtypeSerializers = Array(
        createTypeInformation[ATwo].createSerializer(config)
      ))
    assert(compatibility(a, b).isIncompatible)
  }


  def compatibility(
        writeSerializer: SealedTraitSerializer[ADT],
        readSerializer: SealedTraitSerializer[ADT]) = {
    val buffer = new ByteArrayOutputStream()
    val write = new ScalaSealedTraitSerializerSnapshot[ADT](writeSerializer)
    write.writeSnapshot(new DataOutputViewStreamWrapper(buffer))
    val writeReloaded = new ScalaSealedTraitSerializerSnapshot[ADT]()
    writeReloaded.readSnapshot(
      3,
      new DataInputViewStreamWrapper(new ByteArrayInputStream(buffer.toByteArray)),
      ClassLoader.getSystemClassLoader)
    val result = writeReloaded.resolveSchemaCompatibility(readSerializer)
    result
  }

}

object SealedTraitSerializerCompatibilityTest {
  sealed trait ADT
  case class AOne(a: Int) extends ADT
  case class ATwo(a: String) extends ADT
  case class AThree(a: Long) extends ADT
}
