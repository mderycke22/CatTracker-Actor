package be.unamur.cattracker.actors

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}

import java.net.InetSocketAddress

class NetworkListener(nextActor: ActorRef) extends Actor {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 47474))

  def receive: Receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
  }

  private def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      val processed = socket ! Udp.Send(data, remote) // Send UDP packet to the source
      // Insert into the DB...
      // Publish to MQTT...
      nextActor ! processed // Send the processed data to another actor*
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}