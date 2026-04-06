package arch.core.csr

import arch.core.ooo._
import arch.core.decoder._
import arch.core.imm._
import arch.configs._
import chisel3._

class CsrFU(implicit p: Parameters) extends FunctionalUnit {
  override def desiredName: String = s"${p(ISA).name}_csr_fu"

  val trap_request = IO(Output(Bool()))
  val trap_target  = IO(Output(UInt(p(XLen).W)))
  val trap_ret_tgt = IO(Output(UInt(p(XLen).W)))
  val trap_ret     = IO(Output(Bool()))
  val is_busy      = IO(Output(Bool()))
  val cycle        = IO(Input(UInt(64.W)))
  val instret      = IO(Input(UInt(64.W)))
  val irq          = IO(new CoreInterruptIO)
  val arch_pc      = IO(Input(UInt(p(XLen).W)))

  val csrfile: CsrFile = Module(new CsrFile)
  val decoder          = Module(new Decoder)
  val imm_gen          = Module(new ImmGen)

  val busy    = RegInit(false.B)
  val req_reg = Reg(new MicroOp)

  io.req.ready := !busy || io.resp.ready
  is_busy      := busy

  when(io.flush) {
    busy := false.B
  }.elsewhen(io.req.fire) {
    busy    := true.B
    req_reg := io.req.bits
  }.elsewhen(io.resp.fire) {
    busy := false.B
  }

  decoder.instr   := req_reg.instr
  imm_gen.instr   := req_reg.instr
  imm_gen.immType := decoder.decoded.imm_type

  io.resp.valid        := busy && !io.flush
  io.resp.bits.pc      := req_reg.pc
  io.resp.bits.instr   := req_reg.instr
  io.resp.bits.rd      := req_reg.rd
  io.resp.bits.rob_tag := req_reg.rob_tag

  val active_instr = Mux(busy, req_reg.instr, 0.U)

  csrfile match {
    case csr =>
      csr.en       := true.B
      csr.trap_ret := Mux(busy, decoder.decoded.ret, false.B)
      csr.cmd      := Mux(busy, decoder.decoded.csr_cmd, 0.U)
      csr.addr     := arch.core.csr.CsrUtilitiesFactory.getOrThrow(p(ISA).name).getAddr(active_instr)
      csr.src      := req_reg.rs1_data
      csr.imm      := imm_gen.csr_imm

      csr.pc := Mux(busy, req_reg.pc, arch_pc)

      csr.extraInputIO("cycle")     := cycle
      csr.extraInputIO("instret")   := instret
      csr.extraInputIO("timer_irq") := irq.timer_irq
      csr.extraInputIO("soft_irq")  := irq.soft_irq
      csr.extraInputIO("ext_irq")   := irq.ext_irq

      io.resp.bits.result := csr.rd
      trap_request        := csr.trap_request
      trap_target         := csr.trap_target
      trap_ret_tgt        := csr.trap_ret_target
      trap_ret            := Mux(busy, decoder.decoded.ret, false.B)
  }
}
