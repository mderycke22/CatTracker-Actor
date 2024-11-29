package be.unamur.cattracker

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.util.ByteString
import akka.util.ByteString.ByteStrings
import be.unamur.cattracker.actors.{MqttDeviceActor, SensorValueDbActor}

import scala.concurrent.ExecutionContext
//import akka.actor.{ActorSystem, Props}
import akka.{actor => classic}
import akka.http.scaladsl.Http
import be.unamur.cattracker.actors.{DatabaseAccessActor, DispenserScheduleDbActor, NetworkListenerActor, NetworkSenderActor, RemoteMqttActor}
import be.unamur.cattracker.http.{ApiHttpServer, ApiRoutes, DispenserScheduleService, SensorService}
import be.unamur.cattracker.repositories.{DispenserScheduleRepositoryImpl, SensorRepositoryImpl}
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api.*

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*

object Main {
  private final val conf = ConfigFactory.load()
  private val httpPort = conf.getInt("cat-tracker.http.port")
  private val db = Database.forConfig("cat-tracker.postgres")

  def main(args: Array[String]): Unit = {
    //val typedSystem: TypedActorSystem[Device.Command] = TypedActorSystem(Device("device-1"), "DeviceSystem")
    //implicit val actorSystem: ActorSystem = ActorSystem("CatTrackerSystem")
    //val remoteAddress = InetSocketAddress("localhost", 47474)
    //val remoteMqtt = actorSystem.actorOf(Props(RemoteMqttActor(s"tcp://${mqttAddress}:${mqttPort}")))
    //val networkSender = actorSystem.actorOf(Props(NetworkSenderActor(remoteAddress)), "NetworkSender")
    //val databaseAccess = actorSystem.actorOf(Props(DatabaseAccessActor(SensorRepositoryImpl(db))))
    //val networkListener = actorSystem.actorOf(Props(NetworkListenerActor(networkSender, remoteMqtt, databaseAccess)), "NetworkListener")
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CatTrackerSystem")
    implicit val executionContext: ExecutionContext = system.executionContext

    val dispenserScheduleRepositoryImpl = DispenserScheduleRepositoryImpl(db)
    val sensorValueRepositoryImpl = SensorRepositoryImpl(db)
    val dsDbActor = system.systemActorOf(DispenserScheduleDbActor(dispenserScheduleRepositoryImpl), "DispenserScheduleDbActor")
    val svDbActor = system.systemActorOf(SensorValueDbActor(sensorValueRepositoryImpl), "SensorValueDbActor")
    val remoteMqttActor = system.systemActorOf(MqttDeviceActor("testtopic/a"), "MqttDeviceActor")
    remoteMqttActor ! MqttDeviceActor.MqttSubscribe(println)
    remoteMqttActor ! MqttDeviceActor.MqttPublish(ByteString("Hello from CatTracker!"))
    remoteMqttActor ! MqttDeviceActor.MqttPublish(ByteString("Hello from CatTracker again!"))

    // Http
    val dispenserScheduleService = DispenserScheduleService(dsDbActor)
    val sensorService = SensorService(svDbActor)
    val apiRoutes = ApiRoutes(sensorService, dispenserScheduleService)
    val httpServer = ApiHttpServer(apiRoutes)
    
    httpServer.startServer(httpPort)
  }
}

