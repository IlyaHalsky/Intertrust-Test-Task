package com.intertrust.protocol

import java.time.Instant

trait TurbineCommand

trait WorkerTurbineMove extends TurbineCommand with WindFarmCommand with PersonnelCommand {
  def turbineId: String
  def timestamp: Instant
  def enter: Boolean
}

case class WorkerEnter(turbineId: String, timestamp: Instant) extends WorkerTurbineMove {
  def enter: Boolean = true
}

case class WorkerExit(turbineId: String, timestamp: Instant) extends WorkerTurbineMove {
  def enter: Boolean = false
}