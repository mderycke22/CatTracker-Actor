package be.unamur.cattracker.http


import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.DefaultJsonProtocol.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.unamur.cattracker.actors.DispenserScheduleDbActor.DispenserScheduleDbCommand
import be.unamur.cattracker.actors.SensorValueDbActor
import be.unamur.cattracker.actors.SensorValueDbActor.SensorValueDbCommand
import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleUpdateDTO, SensorValue}
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import spray.json.*
import be.unamur.cattracker.http.LocalDateTimeJsonProtocol.LocalDateTimeFormat
import be.unamur.cattracker.http.LocalTimeJsonProtocol.LocalTimeFormat

import java.time.{LocalDateTime, LocalTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.concurrent.{ExecutionContext, Future}
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
  import akka.http.scaladsl.server.Directives._
  import SensorValueFormat._
  import DispenserScheduleFormat._
  import DispenserScheduleUpdateDTOFormat._

  val apiRoutes: Route = cors() {
    path("api" / "sensor_values" / Segment) { sensorType =>
      get {
        parameters("start_date".optional, "end_date".optional) { (startDateStr, endDateStr) =>
          try {

            // Format yyyy-MM-ddTHH:mm:ss
            val startDateOpt = startDateStr.flatMap(str => Try(LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)).toOption)
            val endDateOpt = endDateStr.flatMap(str => Try(LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)).toOption)

            complete {
              (startDateOpt, endDateOpt) match {
                case (Some(startDate), Some(endDate)) =>
                  sensorService.getAllSensorValuesBetween(sensorType, startDate, endDate).map { sensorValues =>
                    sensorValues.toJson
                  }
                case (Some(startDate), None) =>
                  sensorService.getAllSensorValuesBetween(sensorType, startDate, LocalDateTime.now()).map { sensorValues =>
                    sensorValues.toJson
                  }
                case (None, Some(endDate)) =>
                  sensorService.getAllSensorValuesBetween(sensorType, LocalDateTime.MIN, endDate).map { sensorValues =>
                    sensorValues.toJson
                  }
                case _ =>
                  sensorService.getAllSensorValues(sensorType).map { sensorValues =>
                    sensorValues.toJson
                  }
              }
            }
          } catch {
            case e: DateTimeParseException =>
              complete(StatusCodes.BadRequest, "Invalid date format")
          }
        }
      }
    } ~ path("api" / "dispenser_schedules") {
      concat(get {
        parameters("label_contains".optional) { labelContains =>
          complete {
            labelContains match {
              case Some(value) =>
                dispenserScheduleService.getDispenserSchedules(value).map { dispenserSchedules =>
                  dispenserSchedules.toJson
                }
              case None =>
                dispenserScheduleService.getDispenserSchedules(null).map { dispenserSchedules =>
                  dispenserSchedules.toJson
                }
            }

          }
        }
      },
        post {
          entity(as[DispenserSchedule]) { ds =>
            complete {
              dispenserScheduleService.addDispenserSchedule(ds).map { i =>
                "Dispenser schedule inserted successfully"
              }
            }
          }
        })
    } ~ path("api" / "dispenser_schedules" / Segment) { id =>
      concat(put {
        entity(as[DispenserScheduleUpdateDTO]) { ds =>
          Try(id.toLong).toOption match {
            case Some(_) =>
              complete {
                dispenserScheduleService.updateDispenserSchedule(id.toLong, ds).map { i =>
                  s"Dispenser schedule ${id} updated successfully"
                }
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
                dispenserScheduleService.deleteDispenserSchedule(id.toLong).map { i =>
                  s"Dispenser schedule ${id} deleted successfully"
                }
              }
            case None =>
              complete(StatusCodes.BadRequest, "Invalid id")
          }
        })
    }
  }
}
