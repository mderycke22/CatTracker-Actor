package be.unamur.cattracker.actors

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Actor
import be.unamur.cattracker.model.{DispenserSchedule, SensorValue, SensorValueTable}
import be.unamur.cattracker.repositories.{DispenserScheduleBaseRepository, SensorBaseRepository}

import java.time.LocalDateTime
import scala.util.{Failure, Success}

case class SQLSelectSensors(sensorType: String, start: LocalDateTime, end: LocalDateTime)
case class SQLSelectDispenserSchedule(label: String)
case class SQLInsert(data: Any)

class DatabaseAccessActor(sensorBaseRepository: SensorBaseRepository[SensorValue, Long]) extends Actor {
  import context.system

  def receive: Receive = {
    case SQLInsert(data: String) =>
      val splittedData = splitSensorData(data)

      val sensorValue = valueToFloat(splittedData("value")) match {
        case Some(n) => n
        case None => Float.MinValue
      }

      val sensorValueRow = SensorValue(splittedData("sensor"), sensorValue, splittedData("unit"), LocalDateTime.now())

      sensorBaseRepository.save(sensorValueRow).onComplete {
        case Success(s) => system.log.info(s"Sensor values inserted successfully: ${s}")
        case Failure(t) => system.log.error(s"Error while inserting sensor values: ${t}")
      }
  }

  private def valueToFloat(value: String): Option[Float] = {
    try {
      Some(value.toFloat)
    } catch {
      case _ : NumberFormatException => None
    }
  }

  private def splitSensorData(input: String): Map[String, String] = {
    val parts = input.split(";")

    if (parts.length == 3) {
      Map(
        "sensor" -> parts(0),
        "value" -> parts(1),
        "unit" -> parts(2)
      )
    } else {
      throw new IllegalArgumentException("Invalid string for the sensor")
    }
  }
}