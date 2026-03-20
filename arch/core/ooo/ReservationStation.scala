package arch.core.ooo

import arch.configs._
import chisel3._

class ReservationStation(implicit p: Parameters) extends Module {
  override def desiredName: String = s"${p(ISA)}_reservation_station"
}
