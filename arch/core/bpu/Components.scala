package arch.core.bpu

import arch.configs._
import arch.core.common.Consts
import chisel3.util.BitPat

trait BHTState extends Consts {
  def BHT_X  = BitPat("b??")
  def SZ_BHT = 2

  def BHT_SNT = BitPat("b00")
  def BHT_WNT = BitPat("b01")
  def BHT_WT  = BitPat("b10")
  def BHT_ST  = BitPat("b11")
}

trait BpuUtilities extends Utilities {}

object BpuUtilitiesFactory extends UtilitiesFactory[BpuUtilities]("BPU")

object BpuInit {
  val rv32iUtils = RV32IBpuUtilities
}
