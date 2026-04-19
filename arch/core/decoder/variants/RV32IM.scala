package arch.core.decoder.riscv

import arch.core.mult.riscv._
import arch.core.decoder._
import arch.configs._
import arch.isa._
import chisel3._
import chisel3.util.BitPat

trait RV32IMUOp extends RV32IUOp with RV32IMMultUOpConsts {}

object RV32IMDecoderUtils extends RegisteredUtils[DecoderUtils] with RV32IMUOp {

  private val allEncodings =
    RV32IM.isa.instrSet
      .map(s => s.nop.toSeq ++ s.encodings)
      .getOrElse(Seq.empty)

  private def enc(name: String): BitPat = {
    val e = allEncodings
      .find(_.name == name)
      .getOrElse(throw new NoSuchElementException(s"Instruction '$name' not found in RV32IM"))

    val bits = (p(ILen) - 1 to 0 by -1).map { i =>
      val valueBit = (e.value >> i) & 1
      val maskBit  = (e.mask >> i) & 1
      if (maskBit == 1) valueBit.toString else "?"
    }.mkString

    BitPat("b" + bits)
  }

  override def utils: DecoderUtils = new DecoderUtils {
    override def name: String          = "rv32im"
    override def default: List[BitPat] = RV32IDecoderUtils.utils.default

    override def decode(instr: UInt): DecodedOutput = {
      val sigs    = Wire(new DecodedOutput)
      val decoder = DecodeLogic(instr, default, table)

      val is_div = DecodeLogic(
        instr,
        N,
        Array(
          enc("DIV")  -> Y,
          enc("DIVU") -> Y,
          enc("REM")  -> Y,
          enc("REMU") -> Y,
        )
      ).asBool

      val is_div_signed = DecodeLogic(
        instr,
        N,
        Array(
          enc("DIV") -> Y,
          enc("REM") -> Y,
        )
      ).asBool

      val is_div_rem = DecodeLogic(
        instr,
        N,
        Array(
          enc("REM")  -> Y,
          enc("REMU") -> Y,
        )
      ).asBool

      sigs.legal    := decoder(0).asBool || is_div
      sigs.regwrite := decoder(1).asBool || is_div
      sigs.imm_type := decoder(2)
      sigs.branch   := decoder(3).asBool
      sigs.br_type  := decoder(4)
      sigs.alu      := decoder(5).asBool
      sigs.alu_sel1 := decoder(6)
      sigs.alu_sel2 := decoder(7)
      sigs.alu_mode := decoder(8).asBool
      sigs.alu_fn   := decoder(9)
      sigs.lsu      := decoder(10).asBool
      sigs.lsu_cmd  := decoder(11)
      sigs.csr      := decoder(12).asBool
      sigs.csr_cmd  := decoder(13)

      sigs.mult_en       := decoder(14).asBool
      sigs.mult_high     := decoder(15).asBool
      sigs.mult_a_signed := decoder(16).asBool
      sigs.mult_b_signed := decoder(17).asBool

  sigs.div_en     := is_div
  sigs.div_signed := is_div_signed
  sigs.div_rem    := is_div_rem

      sigs.ret := decoder(18).asBool

      sigs
    }

    override def table: Array[(BitPat, List[BitPat])] = RV32IDecoderUtils.utils.table ++
      Array(
        // R-Type: Mul
        enc("MUL")    -> List(Y, N, IMM_X, N, N, N, N, N, Y, UOP_MUL),
        enc("MULH")   -> List(Y, N, IMM_X, N, N, N, N, N, Y, UOP_MULH),
        enc("MULHSU") -> List(Y, N, IMM_X, N, N, N, N, N, Y, UOP_MULHSU),
        enc("MULHU")  -> List(Y, N, IMM_X, N, N, N, N, N, Y, UOP_MULHU)
      )
  }

  override def factory: UtilsFactory[DecoderUtils] = DecoderUtilsFactory
}
