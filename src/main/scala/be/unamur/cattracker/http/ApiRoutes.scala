package be.unamur.cattracker.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.DefaultJsonProtocol.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.unamur.cattracker.model.SensorValue
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import spray.json.*
import be.unamur.cattracker.http.LocalDateTimeJsonProtocol.LocalDateTimeFormat

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.concurrent.{ExecutionContext, Future}


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

object SensorValueFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sensorFormat: RootJsonFormat[SensorValue] = jsonFormat4(SensorValue)
}

class ApiRoutes(service: SensorService)(implicit ec: ExecutionContext) {
  import akka.http.scaladsl.server.Directives._
  import SensorValueFormat._

  val apiRoutes: Route = path("api" / "sensor_values" / Segment) { sensorType =>
    get {
        parameters("start_date", "end_date") { (startDateStr, endDateStr) =>
          try {
            // Format yyyy-MM-ddTHH:mm:ss
            val startDate = LocalDateTime.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            complete {
                service.getAllSensorValuesBetween(sensorType, startDate, endDate).map { sensorValues =>
                sensorValues.toJson
              }
            }
          } catch {
            case e: DateTimeParseException =>
              complete(StatusCodes.BadRequest, "Invalid date format")
          }
        }
    }
  }
}
