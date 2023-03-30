package com.intertrust.protocol

import java.time.Instant

case object TimeEnd

object TimeEndTypes {
  type TimeEnd = TimeEnd.type
}


case class TimeTick(time: Instant) extends WindFarmCommand with TurbineCommand