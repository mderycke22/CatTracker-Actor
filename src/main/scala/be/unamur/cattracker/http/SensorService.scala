package be.unamur.cattracker.http


import akka.actor.typed.*
import be.unamur.cattracker.actors.SensorValueDbActor
import be.unamur.cattracker.actors.SensorValueDbActor.SensorValueDbCommand
import be.unamur.cattracker.model.SensorValue
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.{ByteString, Timeout}
import be.unamur.cattracker.{CatTrackerConstants, Main}
import be.unamur.cattracker.actors.MqttDeviceActor.{MqttCommand, MqttPublish}

import scala.util.{Failure, Success}
import scala.concurrent.duration.DurationInt
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class SensorService(svDbActor: ActorRef[SensorValueDbCommand], mqttPublishActor: ActorRef[MqttCommand])
                   (implicit val system: ActorSystem[Nothing], val ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds

  def getAllSensorValuesBetween(sensorType: String, start: LocalDateTime, end: LocalDateTime): Future[Seq[SensorValue]] = {
    svDbActor.ask(ref => SensorValueDbActor.Select(sensorType, start, end, ref)).map {
      case SensorValueDbActor.Retrieved(values) =>
        system.log.info("Sensor values retrieved successfully")
        values
      case SensorValueDbActor.NotRetrieved(message) =>
        system.log.error(s"Error getting the sensor values: $message")
        Seq.empty[SensorValue]
    }
  }

  def getAllSensorValues(sensorType: String): Future[Seq[SensorValue]] = {
    svDbActor.ask(ref => SensorValueDbActor.SelectAll(sensorType, ref)).map {
      case SensorValueDbActor.Retrieved(values) =>
        system.log.info("Sensor values retrieved successfully")
        values
      case SensorValueDbActor.NotRetrieved(message) =>
        system.log.error(s"Error getting the sensor values: $message")
        Seq.empty[SensorValue]
    }
  }

  def addSensorValue(sensorValue: SensorValue): Future[Int] = {
    svDbActor.ask(ref => SensorValueDbActor.Insert(sensorValue, ref)).map {
      case SensorValueDbActor.Inserted(values) =>
        system.log.info("Sensor values retrieved successfully")
        values
      case SensorValueDbActor.NotInserted(message) =>
        system.log.error(s"Error getting the sensor values: $message")
        0
    }
  }
  
  def resetWeightSensor(): Unit = {
    mqttPublishActor ! MqttPublish(CatTrackerConstants.publishTopics("weight"), ByteString("reset"))
  }
}
