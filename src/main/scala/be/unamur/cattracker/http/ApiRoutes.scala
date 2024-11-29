package be.unamur.cattracker.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import be.unamur.cattracker.http.LocalDateTimeJsonProtocol.LocalDateTimeFormat
import be.unamur.cattracker.http.LocalTimeJsonProtocol.LocalTimeFormat
import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleUpdateDTO, SensorValue}
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.config.ConfigFactory
import spray.json.*

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{LocalDateTime, LocalTime}
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Used to serialize / deserialize the LocalDateTime type
 */
object LocalDateTimeJsonProtocol extends DefaultJsonProtocol {
  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    def write(obj: LocalDateTime): JsValue = JsString(obj.format(formatter))

    def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) =>
        LocalDateTime.parse(str, formatter)
      case _ =>
        throw DeserializationException("Expected LocalDateTime as JsString")
    }
  }
}

object LocalTimeJsonProtocol extends DefaultJsonProtocol {
  implicit object LocalTimeFormat extends RootJsonFormat[LocalTime] {
    private val formatter = DateTimeFormatter.ISO_TIME

    def write(obj: LocalTime): JsValue = JsString(obj.format(formatter))

    def read(json: JsValue): LocalTime = json match {
      case JsString(str) =>
        LocalTime.parse(str, formatter)
      case _ =>
        throw DeserializationException("Expected LocalTime as JsString")
    }
  }
}

object SensorValueFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sensorFormat: RootJsonFormat[SensorValue] = jsonFormat4(SensorValue)
}

object DispenserScheduleFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val dispenserValueFormat: RootJsonFormat[DispenserSchedule] = jsonFormat4(DispenserSchedule)
}

object DispenserScheduleUpdateDTOFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val dispenserValueFormat: RootJsonFormat[DispenserScheduleUpdateDTO] = jsonFormat3(DispenserScheduleUpdateDTO)
}

class ApiRoutes(sensorService: SensorService, dispenserScheduleService: DispenserScheduleService)(implicit ec: ExecutionContext) {

  import DispenserScheduleFormat.*
  import DispenserScheduleUpdateDTOFormat.*
  import SensorValueFormat.*
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives.*

  private val corsSettings: CorsSettings = CorsSettings(ConfigFactory.load())


  val apiRoutes: Route = cors(corsSettings) {
    pathPrefix("api") {
        path("sensor_values" / Segment) { sensorType =>
          concat(
            options {
              complete(StatusCodes.OK)
            },
            get {
              parameters("start_date".optional, "end_date".optional) { (startDateStr, endDateStr) =>
                try {
                  val startDateOpt = startDateStr.flatMap(str => Try(LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)).toOption)
                  val endDateOpt = endDateStr.flatMap(str => Try(LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)).toOption)

                  complete {
                    (startDateOpt, endDateOpt) match {
                      case (Some(startDate), Some(endDate)) =>
                        sensorService.getAllSensorValuesBetween(sensorType, startDate, endDate).map(_.toJson)
                      case (Some(startDate), None) =>
                        sensorService.getAllSensorValuesBetween(sensorType, startDate, LocalDateTime.now()).map(_.toJson)
                      case (None, Some(endDate)) =>
                        sensorService.getAllSensorValuesBetween(sensorType, LocalDateTime.MIN, endDate).map(_.toJson)
                      case _ =>
                        sensorService.getAllSensorValues(sensorType).map(_.toJson)
                    }
                  }
                } catch {
                  case _: DateTimeParseException =>
                    complete(StatusCodes.BadRequest, "Invalid date format")
                }
              }
            })
        } ~ path("dispenser_schedules") {
          concat(
            options {
              complete(StatusCodes.OK)
            },
            get {
              parameters("label_contains".optional) { labelContains =>
                complete {
                  dispenserScheduleService.getDispenserSchedules(labelContains).map { dispenserSchedules =>
                    dispenserSchedules.toJson
                  }
                }
              }
            },
            post {
              entity(as[DispenserScheduleUpdateDTO]) { ds =>
                complete {
                  dispenserScheduleService.addDispenserSchedule(ds).map { i => "Dispenser schedule inserted successfully" }
                }
              }
            }
          )
        } ~ path("dispenser_schedules" / Segment) { id =>
          concat(
            options {
              complete(StatusCodes.OK)
            },
            put {
              entity(as[DispenserScheduleUpdateDTO]) { ds =>
                Try(id.toLong).toOption match {
                  case Some(_) =>
                    complete {
                      dispenserScheduleService.updateDispenserSchedule(id.toLong, ds).map(_ => s"Dispenser schedule $id updated successfully")
                    }
                  case None =>
                    complete(StatusCodes.BadRequest, "Invalid id")
                }
              }
            },
            delete {
              Try(id.toLong).toOption match {
                case Some(_) =>
                  complete {
                    dispenserScheduleService.deleteDispenserSchedule(id.toLong).map(_ => s"Dispenser schedule $id deleted successfully")
                  }
                case None =>
                  complete(StatusCodes.BadRequest, "Invalid id")
              }
            }
          )
        }
    }
  }
}