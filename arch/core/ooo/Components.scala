package arch.core.ooo

import arch.configs._
import chisel3._
import chisel3.util.{ log2Ceil, BitPat }

class CdbIO(implicit p: Parameters) extends Bundle {}

class ReservationStationEntry(implicit p: Parameters) extends Bundle {}

class RobEntry(implicit p: Parameters) extends Bundle {}
