package arch.core.lsu

import arch.configs._
import arch.configs.proto.FunctionalUnitType._
import vopts.mem.cache._
import chisel3._
import chisel3.util._

class MemoryArbiter(implicit p: Parameters) extends Module {
  override def desiredName: String = s"${p(ISA).name}_memory_arbiter"

  private val NumLDs =
    p(FunctionalUnits).count(_.`type` == FUNCTIONAL_UNIT_TYPE_LD)

  private val NumReqs = NumLDs + 1

  val ld_mem  = IO(Vec(NumLDs, Flipped(new CacheIO(UInt(p(XLen).W), p(XLen)))))
  val ld_mmio = IO(Vec(NumLDs, Flipped(new CacheIO(UInt(p(XLen).W), p(XLen)))))

  val store_mem  = IO(Flipped(new CacheIO(UInt(p(XLen).W), p(XLen))))
  val store_mmio = IO(Flipped(new CacheIO(UInt(p(XLen).W), p(XLen))))

  val mem  = IO(new CacheIO(UInt(p(XLen).W), p(XLen)))
  val mmio = IO(new CacheIO(UInt(p(XLen).W), p(XLen)))

  // Cacheable memory arbiter
  val memArb = Module(new RRArbiter(new CacheReq(UInt(p(XLen).W), p(XLen)), NumReqs))

  for (i <- 0 until NumLDs)
    memArb.io.in(i) <> ld_mem(i).req

  memArb.io.in(NumLDs) <> store_mem.req

  val memRespQ = Module(new Queue(UInt(log2Ceil(NumReqs).W), p(ROBSize)))

  memArb.io.out.ready := mem.req.ready && memRespQ.io.enq.ready
  mem.req.valid       := memArb.io.out.valid && memRespQ.io.enq.ready
  mem.req.bits        := memArb.io.out.bits

  memRespQ.io.enq.valid := memArb.io.out.valid && mem.req.ready
  memRespQ.io.enq.bits  := memArb.io.chosen

  val memTarget = memRespQ.io.deq.bits

  for (i <- 0 until NumLDs) {
    ld_mem(i).resp.valid := mem.resp.valid && memRespQ.io.deq.valid && memTarget === i.U
    ld_mem(i).resp.bits  := mem.resp.bits
  }

  store_mem.resp.valid := mem.resp.valid && memRespQ.io.deq.valid && memTarget === NumLDs.U
  store_mem.resp.bits  := mem.resp.bits

  mem.resp.ready        := false.B
  memRespQ.io.deq.ready := false.B

  when(memRespQ.io.deq.valid) {
    for (i <- 0 until NumLDs)
      when(memTarget === i.U) {
        mem.resp.ready        := ld_mem(i).resp.ready
        memRespQ.io.deq.ready := mem.resp.valid && ld_mem(i).resp.ready
      }

    when(memTarget === NumLDs.U) {
      mem.resp.ready        := store_mem.resp.ready
      memRespQ.io.deq.ready := mem.resp.valid && store_mem.resp.ready
    }
  }

  // MMIO arbiter
  val mmioArb = Module(new RRArbiter(new CacheReq(UInt(p(XLen).W), p(XLen)), NumReqs))

  for (i <- 0 until NumLDs)
    mmioArb.io.in(i) <> ld_mmio(i).req

  mmioArb.io.in(NumLDs) <> store_mmio.req

  val mmioRespQ = Module(new Queue(UInt(log2Ceil(NumReqs).W), p(ROBSize)))

  mmioArb.io.out.ready := mmio.req.ready && mmioRespQ.io.enq.ready
  mmio.req.valid       := mmioArb.io.out.valid && mmioRespQ.io.enq.ready
  mmio.req.bits        := mmioArb.io.out.bits

  mmioRespQ.io.enq.valid := mmioArb.io.out.valid && mmio.req.ready
  mmioRespQ.io.enq.bits  := mmioArb.io.chosen

  val mmioTarget = mmioRespQ.io.deq.bits

  for (i <- 0 until NumLDs) {
    ld_mmio(i).resp.valid := mmio.resp.valid && mmioRespQ.io.deq.valid && mmioTarget === i.U
    ld_mmio(i).resp.bits  := mmio.resp.bits
  }

  store_mmio.resp.valid := mmio.resp.valid && mmioRespQ.io.deq.valid && mmioTarget === NumLDs.U
  store_mmio.resp.bits  := mmio.resp.bits

  mmio.resp.ready        := false.B
  mmioRespQ.io.deq.ready := false.B

  when(mmioRespQ.io.deq.valid) {
    for (i <- 0 until NumLDs)
      when(mmioTarget === i.U) {
        mmio.resp.ready        := ld_mmio(i).resp.ready
        mmioRespQ.io.deq.ready := mmio.resp.valid && ld_mmio(i).resp.ready
      }

    when(mmioTarget === NumLDs.U) {
      mmio.resp.ready        := store_mmio.resp.ready
      mmioRespQ.io.deq.ready := mmio.resp.valid && store_mmio.resp.ready
    }
  }
}
