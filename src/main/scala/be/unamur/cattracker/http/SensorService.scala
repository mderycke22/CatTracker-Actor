package be.unamur.cattracker.http

import akka.actor.ActorSystem
import be.unamur.cattracker.model.SensorValue
import be.unamur.cattracker.repositories.SensorBaseRepository

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class SensorService(sensorBaseRepository: SensorBaseRepository[SensorValue, Long])
                   (implicit val system: ActorSystem, val ec: ExecutionContext) {

  def getAllSensorValuesBetween(sensorType: String, start: LocalDateTime, end: LocalDateTime): Future[Seq[SensorValue]] = {
    sensorBaseRepository.findForSensorBetween(sensorType, start, end)
      .map { s =>
        system.log.info(s"Sensor values between $start and $end for $sensorType retrieved successfully")
        s
      }
      .recover { case t: Throwable =>
        system.log.error(s"Error getting the sensor values: $t")
        Seq.empty[SensorValue]
      }
  }

  def addSensorValue(sensorValue: SensorValue): Future[Int] = {
    sensorBaseRepository.save(sensorValue)
      .map { i =>
        system.log.info(s"Sensor value added successfully ($i value added)")
        i
      }
      .recover { case t: Throwable =>
        system.log.error(s"Couldn't insert the sensor value: $t")
        0
      }
  }
}
