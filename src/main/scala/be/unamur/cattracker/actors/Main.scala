package be.unamur.cattracker.actors

import akka.actor.{ActorSystem, Props}
import akka.actor.typed.ActorSystem as TypedActorSystem
import akka.http.scaladsl.Http
import be.unamur.cattracker.actors.{NetworkListener, NetworkSender}
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.ExecutionContext.Implicits.global
import java.net.InetSocketAddress
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import be.unamur.cattracker.http.{ApiHttpServer, ApiRoutes, SensorService}
import be.unamur.cattracker.repositories.SensorRepositoryImpl

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
    val remoteMqtt = actorSystem.actorOf(Props(RemoteMqtt(s"tcp://${mqttAddress}:${mqttPort}")))
    val networkSender = actorSystem.actorOf(Props(NetworkSender(remoteAddress)), "NetworkSender")
    val databaseAccess = actorSystem.actorOf(Props(DatabaseAccess(SensorRepositoryImpl(db))))
    val networkListener = actorSystem.actorOf(Props(NetworkListener(networkSender, remoteMqtt, databaseAccess)), "NetworkListener")

    // Http
    val sensorService = SensorService(SensorRepositoryImpl(db))
    val apiRoutes = ApiRoutes(sensorService)
    val httpServer = ApiHttpServer(apiRoutes)
    
    httpServer.startServer(httpPort)
  }
}

