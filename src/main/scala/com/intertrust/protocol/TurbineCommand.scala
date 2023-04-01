package com.intertrust.protocol

import java.time.Instant

trait TurbineCommand

trait WorkerTurbineMove extends TurbineCommand with WindFarmCommand with PersonnelCommand {
  def personId: String
  def turbineId: String
  def timestamp: Instant
  def enter: Boolean
}

case class WorkerEnterTurbine(personId: String, turbineId: String, timestamp: Instant) extends WorkerTurbineMove {
  def enter: Boolean = true
}

case class WorkerExitTurbine(personId: String, turbineId: String, timestamp: Instant) extends WorkerTurbineMove {
  def enter: Boolean = false
}