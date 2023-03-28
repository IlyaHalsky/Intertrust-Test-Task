package com.intertrust.protocol

import java.time.Instant

sealed trait Alert extends PersistableEvent

case class TurbineAlert(timestamp: Instant, turbineId: String, error: String) extends Alert with WindFarmCommand

case class MovementAlert(timestamp: Instant, engineerId: String, error: String) extends Alert with PersonnelCommand
