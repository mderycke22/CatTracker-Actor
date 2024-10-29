package be.unamur.cattracker.actors


import akka.actor.{ActorSystem, Props}
import akka.actor.typed.ActorSystem as TypedActorSystem
import be.unamur.cattracker.actors.{NetworkListener, NetworkSender}
import com.typesafe.config.ConfigFactory

import java.net.InetSocketAddress
import scala.util.{Failure, Success}

object Main {
  private final val conf = ConfigFactory.load()
  private val mqttPort = conf.getLong("cat-tracker.mqtt.port")
  private val mqttAddress = conf.getString("cat-tracker.mqtt.ip")


  def main(args: Array[String]): Unit = {
    //val typedSystem: TypedActorSystem[Device.Command] = TypedActorSystem(Device("device-1"), "DeviceSystem")
    val untypedSystem: ActorSystem = ActorSystem("NetworkSystem")
    val remoteAddress = InetSocketAddress("localhost", 47474)
    val remoteMqtt = untypedSystem.actorOf(Props(RemoteMqtt(s"tcp://${mqttAddress}:${mqttPort}")))
    val networkSender = untypedSystem.actorOf(Props(NetworkSender(remoteAddress)), "NetworkSender")
    val networkListener = untypedSystem.actorOf(Props(NetworkListener(networkSender, remoteMqtt)), "NetworkListener")
  }
}

