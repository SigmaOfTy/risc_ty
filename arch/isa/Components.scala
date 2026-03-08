package arch.isa

import arch.configs.proto._
import chisel3.util.BitPat
import scala.collection.mutable.LinkedHashMap

abstract class IsaFactory {
  def toIsa: Isa

  final def name: String         = toIsa.name
  final def xlen: Int            = toIsa.xlen.toInt
  final def ilen: Int            = toIsa.ilen.toInt
  final def numArchRegs: Int     = toIsa.numArchRegs.toInt
  final def isBigEndian: Boolean = toIsa.isBigEndian

  final def bubble: BitPat = {
    val nop = toIsa.instrSet
      .flatMap(_.nop)
      .getOrElse(throw new Exception(s"ISA '$name' has no NOP defined"))

    val bits = (ilen - 1 to 0 by -1).map { i =>
      val valueBit = (nop.value >> i) & 1
      val maskBit  = (nop.mask >> i) & 1
      if (maskBit == 1) valueBit.toString else "?"
    }.mkString

    BitPat("b" + bits)
  }

}

object IsaDef {
  private val registry = LinkedHashMap.empty[String, IsaFactory]

  def register(isa: IsaFactory): Unit = {
    require(!registry.contains(isa.name), s"ISA '${isa.name}' already registered")
    registry(isa.name) = isa
  }

  def fromString(name: String): Option[IsaFactory] =
    registry.get(name.toLowerCase)

  def available: Seq[IsaFactory] = registry.values.toSeq

  private def get(name: String): IsaFactory =
    fromString(name).getOrElse(
      throw new Exception(
        s"Unknown Isa: '$name'. Available: ${available.map(_.name).mkString(", ")}"
      )
    )

  def xlen(isa: String): Int            = get(isa).xlen
  def ilen(isa: String): Int            = get(isa).ilen
  def numArchRegs(isa: String): Int     = get(isa).numArchRegs
  def isBigEndian(isa: String): Boolean = get(isa).isBigEndian
  def bubble(isa: String): BitPat       = get(isa).bubble
  def toIsa(isa: String): Isa           = get(isa).toIsa
}
