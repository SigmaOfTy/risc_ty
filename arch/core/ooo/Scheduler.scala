package arch.core.ooo

import arch.configs._
import chisel3._
import chisel3.util.{ Decoupled, Valid }

abstract class Scheduler(implicit p: Parameters) extends Module {
  val numFUs     = p(FunctionalUnits).size
  val issueWidth = p(IssueWidth)

  val dis_reqs = IO(Vec(issueWidth, Flipped(Decoupled(new MicroOp))))

  val fu_reqs  = IO(Vec(numFUs, Decoupled(new MicroOp)))
  val fu_resps = IO(Flipped(Vec(numFUs, Valid(new FunctionalUnitResp))))
}

object SchedulerFactory {
  def apply()(implicit p: Parameters): Scheduler =
    p(ScheduleType) match {
      // case "scoreboard" => Module(new scoreboard.SuperscalarScoreboard)
      // case "tomasulo" => Module(new tomasulo.TomasuloScheduler)
      // case "inorder"  => Module(new inorder.InOrderScheduler)
      case other => throw new IllegalArgumentException(s"Unknown Scheduler type: $other")
    }
}
