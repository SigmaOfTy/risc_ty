package arch.core.issue

import arch.configs._
import arch.configs.proto.FunctionalUnitType
import chisel3._
import chisel3.util.{ log2Ceil, PopCount, MuxLookup }
import scala.math.max

class IssueUnit(implicit p: Parameters) extends Module {
  override def desiredName: String = s"${p(ISA).name}_issue_unit"

  private val NumFUs = p(FunctionalUnits).size
  private val FuIdW  = log2Ceil(NumFUs)
  private val FuTypW = log2Ceil(FunctionalUnitType.values.size)

  val inst_type      = IO(Input(Vec(p(IssueWidth), UInt(FuTypW.W))))
  val wants_to_issue = IO(Input(Vec(p(IssueWidth), Bool())))
  val intra_hazard   = IO(Input(Vec(p(IssueWidth), Bool())))

  val struct_hazard = IO(Output(Vec(p(IssueWidth), Bool())))
  val target_fu_id  = IO(Output(Vec(p(IssueWidth), UInt(FuIdW.W))))

  private def typeU(t: FunctionalUnitType): UInt =
    t.index.U(FuTypW.W)

  private def getIds(fuType: FunctionalUnitType): Seq[UInt] =
    p(FunctionalUnits).zipWithIndex
      .filter(_._1.`type` == fuType)
      .map(_._2.U(FuIdW.W))

  val aluIds  = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ALU)
  val multIds = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_MULT)
  val divIds  = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_DIV)
  val ldIds   = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_LD)
  val stIds   = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ST)
  val bruIds  = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_BRU)
  val csrIds  = getIds(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_CSR)

  private def rrWidth(n: Int): Int =
    log2Ceil(max(n, 1) + 1)

  val alu_rr = RegInit(0.U(rrWidth(aluIds.length).W))
  val ld_rr  = RegInit(0.U(rrWidth(ldIds.length).W))
  val st_rr  = RegInit(0.U(rrWidth(stIds.length).W))

  private def getId(used: UInt, ids: Seq[UInt], rr: UInt = 0.U): UInt =
    if (ids.isEmpty) {
      0.U(FuIdW.W)
    } else {
      val idx = (used + rr) % ids.length.U
      MuxLookup(idx, ids.head)(
        ids.zipWithIndex.map { case (id, i) => i.U -> id }
      )
    }

  private def countUsed(w: Int, fuType: UInt): UInt =
    PopCount((0 until w).map { i =>
      wants_to_issue(i) &&
      !intra_hazard(i) &&
      inst_type(i) === fuType &&
      !struct_hazard(i)
    })

  for (w <- 0 until p(IssueWidth)) {
    val alu_used  = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ALU))
    val ld_used   = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_LD))
    val st_used   = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ST))
    val div_used  = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_DIV))
    val mult_used = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_MULT))
    val bru_used  = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_BRU))
    val csr_used  = countUsed(w, typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_CSR))

    struct_hazard(w) := MuxLookup(inst_type(w), false.B)(
      Seq(
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ALU)  -> (alu_used >= aluIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_LD)   -> (ld_used >= ldIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ST)   -> (st_used >= stIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_DIV)  -> (div_used >= divIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_MULT) -> (mult_used >= multIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_BRU)  -> (bru_used >= bruIds.length.U),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_CSR)  -> (csr_used >= csrIds.length.U)
      )
    )

    target_fu_id(w) := MuxLookup(inst_type(w), getId(alu_used, aluIds, alu_rr))(
      Seq(
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ALU)  -> getId(alu_used, aluIds, alu_rr),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_LD)   -> getId(ld_used, ldIds, ld_rr),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ST)   -> getId(st_used, stIds, st_rr),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_DIV)  -> getId(div_used, divIds),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_MULT) -> getId(mult_used, multIds),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_BRU)  -> getId(bru_used, bruIds),
        typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_CSR)  -> getId(csr_used, csrIds)
      )
    )
  }

  val alu_disp = PopCount((0 until p(IssueWidth)).map { w =>
    wants_to_issue(w) &&
    !intra_hazard(w) &&
    inst_type(w) === typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ALU) &&
    !struct_hazard(w)
  })

  val ld_disp = PopCount((0 until p(IssueWidth)).map { w =>
    wants_to_issue(w) &&
    !intra_hazard(w) &&
    inst_type(w) === typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_LD) &&
    !struct_hazard(w)
  })

  val st_disp = PopCount((0 until p(IssueWidth)).map { w =>
    wants_to_issue(w) &&
    !intra_hazard(w) &&
    inst_type(w) === typeU(FunctionalUnitType.FUNCTIONAL_UNIT_TYPE_ST) &&
    !struct_hazard(w)
  })

  if (aluIds.nonEmpty) {
    alu_rr := (alu_rr + alu_disp) % aluIds.length.U
  }

  if (ldIds.nonEmpty) {
    ld_rr := (ld_rr + ld_disp) % ldIds.length.U
  }

  if (stIds.nonEmpty) {
    st_rr := (st_rr + st_disp) % stIds.length.U
  }
}
