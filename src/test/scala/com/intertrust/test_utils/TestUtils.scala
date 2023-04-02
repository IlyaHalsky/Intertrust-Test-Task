package com.intertrust.test_utils

import com.intertrust.protocol.Movement.{Enter, Exit}
import com.intertrust.protocol.TurbineStatus.{Broken, Working}
import com.intertrust.protocol.{Movement, MovementAlert, MovementEvent, TimeTick, Turbine, TurbineAlert, TurbineEvent, TurbineStatus, Vessel, WorkerEnterTurbine, WorkerExitTurbine, WorkerTurbineMove}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

trait TestUtils {
  val testStart: Instant = Instant.now()

  def plus(duration: FiniteDuration): Instant =
    testStart + duration

  def plus(millis: Long): Instant =
    testStart + millis

  val kafkaId = "test-kafka"
  val engineerId = "test-engineer"
  val turbineId = "test-turbine"
  val personnelId = "test-personnel"
  val windFarmId = "test-wind-farm"
  val vesselId = "test-vessel"
  val invalidVesselId = "invalid-vessel"

  def moveVessel(offset: Long = 0, enter: Movement = Enter, vesselId: String = vesselId): MovementEvent =
    MovementEvent(engineerId, Vessel(vesselId), enter, plus(offset))

  def moveTurbine(offset: Long = 0, enter: Movement = Enter): MovementEvent =
    MovementEvent(engineerId, Turbine(turbineId), enter, plus(offset))

  def movementAlert(offset: Long, error: String): MovementAlert =
    MovementAlert(plus(offset), engineerId, error)

  def workerMoveTurbine(offset: Long = 0, enter: Movement = Enter): WorkerTurbineMove =
    if (enter == Enter) WorkerEnterTurbine(engineerId, turbineId, plus(offset))
    else WorkerExitTurbine(engineerId, turbineId, plus(offset))

  def turbineBroke(offset: Long = 0): TurbineEvent =
    TurbineEvent(turbineId, Broken, 0.0, plus(offset))

  def turbineWorking(offset: Long = 0): TurbineEvent =
    TurbineEvent(turbineId, Working, 0.0, plus(offset))

  def turbineAlert(offset: Long, error: String): TurbineAlert =
    TurbineAlert(plus(offset), turbineId, error)
}
