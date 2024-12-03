package be.unamur.cattracker

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.util.ByteString
import be.unamur.cattracker.actors.{MqttDeviceActor, SensorValueDbActor}

import scala.concurrent.{ExecutionContext, Future}
import akka.actor as classic
import be.unamur.cattracker.actors.MqttDeviceActor.{MqttCommand, MqttPublish, MqttSubscribe}
import be.unamur.cattracker.actors.DispenserScheduleDbActor
import be.unamur.cattracker.http.{ApiHttpServer, ApiRoutes, DispenserScheduleService, SensorService}
import be.unamur.cattracker.model.SensorValue
import be.unamur.cattracker.repositories.{DispenserScheduleRepositoryImpl, SensorRepositoryImpl}
import be.unamur.cattracker.utils.DataUtils
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api.*
import akka.{Done, NotUsed}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.alpakka.mqtt.scaladsl.{MqttFlow, MqttMessageWithAck, MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import be.unamur.cattracker.Main.conf
import com.typesafe.config.ConfigFactory
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import akka.stream.scaladsl.MergeHub.source

import java.time.LocalDateTime

object Main {
  private final val conf = ConfigFactory.load()
  private val httpAddress = conf.getString("cat-tracker.http.ip")
  private val httpPort = conf.getInt("cat-tracker.http.port")
  private val db = Database.forConfig("cat-tracker.postgres")
  private val mqttPort = conf.getLong("cat-tracker.mqtt.port")
  private val mqttAddress = conf.getString("cat-tracker.mqtt.ip")
  private val brokerUrl = s"tcp://${mqttAddress}:${mqttPort}"


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CatTrackerSystem")
    implicit val executionContext: ExecutionContext = system.executionContext

    val dispenserScheduleRepositoryImpl = DispenserScheduleRepositoryImpl(db)
    val sensorValueRepositoryImpl = SensorRepositoryImpl(db)
    val dsDbActor = system.systemActorOf(DispenserScheduleDbActor(dispenserScheduleRepositoryImpl), "DispenserScheduleDbActor")
    val svDbActor = system.systemActorOf(SensorValueDbActor(sensorValueRepositoryImpl), "SensorValueDbActor")

    // Mqtt
    val connectionSettings: MqttConnectionSettings = MqttConnectionSettings(
      brokerUrl,
      "cattracker-backend",
      new MemoryPersistence,
    ).withCleanSession(true)
      .withAutomaticReconnect(true)

    val subscriptionsMap: Map[String, MqttQoS] = CatTrackerConstants.subscribeTopics.map(topic => topic -> MqttQoS.AtLeastOnce).toMap

    val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(connectionSettings, MqttQoS.AtLeastOnce)
    val mqttSource: Source[MqttMessage, Future[Done]] = {
      MqttSource.atMostOnce(
        connectionSettings.withClientId(clientId = "cattracker/backend"),
        MqttSubscriptions(subscriptionsMap),
        bufferSize = 8
      )
    }
    val mqttActor = system.systemActorOf(MqttDeviceActor(mqttSink, mqttSource), "MqttActor")

    // Http and services
    val dispenserScheduleService = DispenserScheduleService(dsDbActor, mqttActor)
    val sensorService = SensorService(svDbActor, mqttActor)
    val apiRoutes = ApiRoutes(sensorService, dispenserScheduleService)
    val httpServer = ApiHttpServer(apiRoutes)

    mqttActor ! MqttSubscribe(message => sensorValueSubscriptionCallback(message, sensorService))
    dispenserScheduleService.sendAllDistributionSchedules()

    httpServer.startServer(httpAddress, httpPort)
  }

  private def sensorValueSubscriptionCallback(message: String, sensorService: SensorService) = {
    val sensorData = DataUtils.splitSensorData(message)
    val sensorValue = SensorValue(sensorData("sensor"), DataUtils.castToFloat(sensorData("value")), sensorData("unit"), LocalDateTime.now())
    sensorService.addSensorValue(sensorValue)
  }
}

