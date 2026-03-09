package arch.core.ifu

import arch.configs._
import chisel3._
import chisel3.util._

class IBufferEntry(implicit p: Parameters) extends Bundle {
  val pc    = UInt(p(XLen).W)
  val instr = UInt(p(ILen).W)
}

class IBuffer(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val enq   = Flipped(Decoupled(new IBufferEntry))
    val deq   = Decoupled(new IBufferEntry)
    val count = Output(UInt(log2Ceil(p(IBufferSize) + 1).W))
  })

  val q = Module(new Queue(new IBufferEntry, p(IBufferSize)))

  q.io.enq <> io.enq
  io.deq <> q.io.deq
  io.count := q.io.count
}
