package be.unamur.cattracker.actors


import akka.actor.{ActorSystem, Props}
import akka.actor.typed.{ActorSystem => TypedActorSystem}
import be.unamur.cattracker.actors.{NetworkListener, NetworkSender}

import java.net.InetSocketAddress
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    //val typedSystem: TypedActorSystem[Device.Command] = TypedActorSystem(Device("device-1"), "DeviceSystem")
    val untypedSystem: ActorSystem = ActorSystem("NetworkSystem")
    val remoteAddress = InetSocketAddress("localhost", 47474)
    val networkSender = untypedSystem.actorOf(Props(NetworkSender(remoteAddress)), "NetworkSender")
    val networkListener = untypedSystem.actorOf(Props(NetworkListener(networkSender)), "NetworkListener")
  }
}

