package arch.core.lsu

import arch.core.ooo._
import arch.core.imm._
import arch.core.pma._
import arch.configs._
import chisel3._
import chisel3.util._

object StoreFUState extends ChiselEnum {
  val IDLE, WRITE_SB, DONE = Value
}

class StoreFU(implicit p: Parameters) extends FunctionalUnit {
  override def desiredName: String = s"${p(ISA).name}_store_fu"

  val sbWrite = IO(Valid(new StoreWriteBundle))
  val busy    = IO(Output(Bool()))

  val utils    = LsuUtilsFactory.getOrThrow(p(ISA).name)
  val immUtils = ImmUtilsFactory.getOrThrow(p(ISA).name)

  val state  = RegInit(StoreFUState.IDLE)
  val uopReg = Reg(new MicroOp)

  val ctrl = utils.decode(uopReg.uop)
  val imm  = immUtils.genImm(uopReg.instr, uopReg.imm_type)
  val addr = uopReg.rs1_data + imm

  val alignedAddr = LsuData.alignedAddr(addr)
  val storeData   = LsuData.alignedStoreData(ctrl, addr, uopReg.rs2_data)
  val storeMask   = LsuData.shiftedMask(ctrl, addr)

  val (_, _, pmaWritable, pmaCacheable) = PmaChecker(addr)

  busy := state =/= StoreFUState.IDLE

  io.req.ready := state === StoreFUState.IDLE

  sbWrite.valid          := state === StoreFUState.WRITE_SB
  sbWrite.bits.sq_idx    := uopReg.sq_idx
  sbWrite.bits.rob_tag   := uopReg.rob_tag
  sbWrite.bits.addr      := alignedAddr
  sbWrite.bits.data      := storeData
  sbWrite.bits.mask      := storeMask
  sbWrite.bits.cacheable := pmaCacheable

  io.resp.valid        := state === StoreFUState.DONE
  io.resp.bits.result  := 0.U
  io.resp.bits.rd      := 0.U
  io.resp.bits.pc      := uopReg.pc
  io.resp.bits.instr   := uopReg.instr
  io.resp.bits.rob_tag := uopReg.rob_tag

  when(io.flush) {
    state := StoreFUState.IDLE
  }.otherwise {
    switch(state) {
      is(StoreFUState.IDLE) {
        when(io.req.fire) {
          uopReg := io.req.bits
          state  := StoreFUState.WRITE_SB
        }
      }

      is(StoreFUState.WRITE_SB) {
        state := StoreFUState.DONE
      }

      is(StoreFUState.DONE) {
        when(io.resp.fire) {
          state := StoreFUState.IDLE
        }
      }
    }
  }
}
