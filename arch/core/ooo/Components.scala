package arch.core.ooo

import arch.configs._
import chisel3._
import chisel3.util.{ log2Ceil, BitPat }

trait PipelineStageConsts {
  def PS_X   = BitPat("b???")
  def SZ_PS  = PS_X.getWidth
  def PS_IF  = BitPat("b000")
  def PS_ID  = BitPat("b001")
  def PS_EX  = BitPat("b010")
  def PS_MEM = BitPat("b011")
  def PS_WB  = BitPat("b100")
}

class ROBEntry(implicit p: Parameters) extends Bundle with PipelineStageConsts {
  val busy  = Bool()
  val instr = UInt(p(ILen).W)
  val stage = UInt(SZ_PS.W)
  val dest  = UInt(log2Ceil(p(NumPhyRegs)).W)
  val value = UInt(p(XLen).W)
}
