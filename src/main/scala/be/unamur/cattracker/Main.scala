package be.unamur.cattracker

import akka.actor.typed.ActorSystem as TypedActorSystem
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import be.unamur.cattracker.actors.{DatabaseAccessActor, NetworkListenerActor, NetworkSenderActor, RemoteMqttActor}
import be.unamur.cattracker.http.{ApiHttpServer, ApiRoutes, DispenserScheduleService, SensorService}
import be.unamur.cattracker.repositories.{DispenserScheduleRepositoryImpl, SensorRepositoryImpl}
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api.*

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.*

object Main {
  private final val conf = ConfigFactory.load()
  private val mqttPort = conf.getLong("cat-tracker.mqtt.port")
  private val mqttAddress = conf.getString("cat-tracker.mqtt.ip")
  private val httpPort = conf.getInt("cat-tracker.http.port")
  private val db = Database.forConfig("cat-tracker.postgres")

  def main(args: Array[String]): Unit = {
    //val typedSystem: TypedActorSystem[Device.Command] = TypedActorSystem(Device("device-1"), "DeviceSystem")
    implicit val actorSystem: ActorSystem = ActorSystem("CatTrackerSystem")
    val remoteAddress = InetSocketAddress("localhost", 47474)
    val remoteMqtt = actorSystem.actorOf(Props(RemoteMqttActor(s"tcp://${mqttAddress}:${mqttPort}")))
    val networkSender = actorSystem.actorOf(Props(NetworkSenderActor(remoteAddress)), "NetworkSender")
    val databaseAccess = actorSystem.actorOf(Props(DatabaseAccessActor(SensorRepositoryImpl(db))))
    val networkListener = actorSystem.actorOf(Props(NetworkListenerActor(networkSender, remoteMqtt, databaseAccess)), "NetworkListener")

    // Http
    val dispenserScheduleService = DispenserScheduleService(DispenserScheduleRepositoryImpl(db))
    val sensorService = SensorService(SensorRepositoryImpl(db))
    val apiRoutes = ApiRoutes(sensorService, dispenserScheduleService)
    val httpServer = ApiHttpServer(apiRoutes)
    
    httpServer.startServer(httpPort)
  }
}

