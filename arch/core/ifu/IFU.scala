package arch.core.ifu

import chisel3._
import arch.configs._

class IFU(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val icache_req_valid = Output(Bool())
    val icache_req_ready = Input(Bool())
    val icache_req_addr  = Output(UInt(p(XLen).W))

    val icache_resp_valid = Input(Bool())
    val icache_resp_ready = Output(Bool())
    val icache_resp_data  = Input(UInt(p(XLen).W))

    val bru_taken       = Input(Bool())
    val bru_target      = Input(UInt(p(XLen).W))
    val id_ex_stall     = Input(Bool())
    val load_use_hazard = Input(Bool())
    val lsu_busy        = Input(Bool())

    val if_id_stall = Output(Bool())
    val if_id_flush = Output(Bool())
    val if_instr    = Output(UInt(p(ILen).W))
    val if_pc       = Output(UInt(p(XLen).W))

    val ibuffer_deq_fire = Output(Bool())
    val reset_ibuffer    = Output(Bool())
  })

  val ibuffer = Module(new IBuffer)

  val pc = RegInit(0.U(p(XLen).W))

  val reset_ibuffer = RegInit(false.B)
  val ibuffer_empty = ibuffer.io.count === 0.U
  val ibuffer_full  = ibuffer.io.count === p(IBufferSize).U

  val imem_pending = RegInit(false.B)
  val imem_data    = RegInit(p(Bubble).value.U(p(ILen).W))
  val imem_pc      = RegInit(0.U(p(XLen).W))
  val imem_valid   = RegInit(false.B)

  io.icache_req_valid  := !imem_pending && !ibuffer_full
  io.icache_req_addr   := pc
  io.icache_resp_ready := true.B

  val icache_req_fire  = io.icache_req_valid && io.icache_req_ready
  val icache_resp_fire = io.icache_resp_valid && io.icache_resp_ready

  when(icache_req_fire) {
    imem_pending := true.B
    imem_pc      := pc
    imem_valid   := true.B
  }

  when(io.bru_taken) {
    imem_valid := false.B
  }

  when(icache_resp_fire) {
    imem_data    := io.icache_resp_data
    imem_pending := false.B
  }

  when(reset_ibuffer) {
    imem_valid := false.B
  }
  when(io.bru_taken) {
    reset_ibuffer := true.B
  }
  when(ibuffer_empty && !imem_pending) {
    reset_ibuffer := false.B
  }

  ibuffer.io.enq.valid      := icache_resp_fire && imem_valid && !ibuffer_full
  ibuffer.io.enq.bits.pc    := imem_pc
  ibuffer.io.enq.bits.instr := io.icache_resp_data

  val stall_cond = io.id_ex_stall || io.load_use_hazard
  val flush_cond = (io.bru_taken || !imem_valid || reset_ibuffer) && !io.lsu_busy

  ibuffer.io.deq.ready := (!ibuffer_empty && !stall_cond && !flush_cond) || reset_ibuffer

  io.if_id_stall := stall_cond
  io.if_id_flush := flush_cond
  io.if_instr    := Mux(ibuffer.io.deq.fire, ibuffer.io.deq.bits.instr, p(Bubble).value.U(p(ILen).W))
  io.if_pc       := Mux(ibuffer.io.deq.fire, ibuffer.io.deq.bits.pc, p(Bubble).value.U(p(XLen).W))

  io.ibuffer_deq_fire := ibuffer.io.deq.fire
  io.reset_ibuffer    := reset_ibuffer

  when(io.bru_taken && !io.lsu_busy) {
    pc := io.bru_target
  }.elsewhen(ibuffer.io.enq.fire) {
    pc := pc + 4.U(p(XLen).W)
  }
}
