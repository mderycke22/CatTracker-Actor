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
      // TODO: check for a better way to do it with monads
      val sensorValue = valueToFloat(data) match {
        case Some(n) => n
        case None => Float.MinValue
      }

      val sensorValueRow = SensorValue("TestSensor",  sensorValue, LocalDateTime.now())
      val insertSensorValueQuery = sensorValuesTable += sensorValueRow
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
}