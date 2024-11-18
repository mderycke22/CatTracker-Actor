package be.unamur.cattracker.actors

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Actor
import be.unamur.cattracker.model.{SensorValue, SensorValueTable}
import slick.jdbc.PostgresProfile.api.*

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.util.{Success, Failure}

case class SQLSelect(data: String)
case class SQLInsert(data: String)

class DatabaseAccess extends Actor {
  import context.system

  private val db = Database.forConfig("cat-tracker.postgres")
  private val sensorValuesTable = TableQuery[SensorValueTable]

  def receive: Receive = {
    // TODO: data should also contain the sensor type
    case SQLInsert(data: String) =>
      val splittedData = splitSensorData(data)

      val sensorValue = valueToFloat(splittedData("value")) match {
        case Some(n) => n
        case None => Float.MinValue
      }

      val sensorValueRow = SensorValue(splittedData("sensor"), sensorValue, splittedData("unit"), LocalDateTime.now())
      val insertSensorValueQuery = sensorValuesTable += sensorValueRow
      // TODO: use repository or service
      val insertResult: Future[Int] = db.run(insertSensorValueQuery)

      insertResult.onComplete {
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