package be.unamur.cattracker

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.util.ByteString
import be.unamur.cattracker.actors.{MqttDeviceActor, SensorValueDbActor}

import scala.concurrent.ExecutionContext
import akka.actor as classic
import be.unamur.cattracker.actors.MqttDeviceActor.{MqttPublish, MqttSubscribe}
import be.unamur.cattracker.actors.DispenserScheduleDbActor
import be.unamur.cattracker.http.{ApiHttpServer, ApiRoutes, DispenserScheduleService, SensorService}
import be.unamur.cattracker.model.SensorValue
import be.unamur.cattracker.repositories.{DispenserScheduleRepositoryImpl, SensorRepositoryImpl}
import be.unamur.cattracker.utils.DataUtils
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api.*

import java.time.LocalDateTime

object Main {
  private final val conf = ConfigFactory.load()
  private val httpAddress = conf.getString("cat-tracker.http.ip")
  private val httpPort = conf.getInt("cat-tracker.http.port")
  private val db = Database.forConfig("cat-tracker.postgres")

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CatTrackerSystem")
    implicit val executionContext: ExecutionContext = system.executionContext

    val dispenserScheduleRepositoryImpl = DispenserScheduleRepositoryImpl(db)
    val sensorValueRepositoryImpl = SensorRepositoryImpl(db)
    val dsDbActor = system.systemActorOf(DispenserScheduleDbActor(dispenserScheduleRepositoryImpl), "DispenserScheduleDbActor")
    val svDbActor = system.systemActorOf(SensorValueDbActor(sensorValueRepositoryImpl), "SensorValueDbActor")

    // Http
    val dispenserScheduleService = DispenserScheduleService(dsDbActor)
    val sensorService = SensorService(svDbActor)
    val apiRoutes = ApiRoutes(sensorService, dispenserScheduleService)
    val httpServer = ApiHttpServer(apiRoutes)

    // Mqtt
    val mqttWeightValuesActor = system.systemActorOf(MqttDeviceActor("cattracker/weight/sensor_outputs"), "MqttWeightActor")
    val mqttWeightResetActor = system.systemActorOf(MqttDeviceActor("cattracker/weight/reset"), "MqttWeightResetActor")

    val mqttTemperatureHumidityActor = system.systemActorOf(MqttDeviceActor("cattracker/temp_hum/sensor_outputs"), "MqttTempHumActor")

    val mqttKibblesDistribActor = system.systemActorOf(MqttDeviceActor("cattracker/kibbles/distribution"), "MqttDistribActor")

    val mqttTemperatureHumidityBackActor = system.systemActorOf(MqttDeviceActor("cattracker/temp_hum/backend_output"), "MqttTempHumBackActor")
    val mqttWeightBackActor = system.systemActorOf(MqttDeviceActor("cattracker/weight/backend_output"), "MqttWeightBackActor")

    mqttWeightValuesActor ! MqttSubscribe(message => {
      val sensorData = DataUtils.splitSensorData(message)
      val sensorValue = SensorValue(sensorData("sensor"), DataUtils.castToFloat(sensorData("value")), sensorData("unit"), LocalDateTime.now())
      sensorService.addSensorValue(sensorValue)
      mqttWeightBackActor ! MqttPublish(ByteString(message))
    })
    
    httpServer.startServer(httpAddress, httpPort)
  }
}

