package be.unamur.cattracker.actors

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}

import java.net.InetSocketAddress

class NetworkListener(nextActor: ActorRef, mqttActor: ActorRef) extends Actor {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 47474))


  def receive: Receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
  }

  private def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      /*
       * In this case, the Listener is the server (using MQTT, inserting into the database, ...)
       * If an actuator should be a NetworkListener, we need to add a notion of role played by the listener
       * or create another class.
       */ 
      context.system.log.info("Data received: {}", data.decodeString("ISO-8859-1"))
      val processed = socket ! Udp.Send(data, remote) // Send UDP packet to the source
      // Insert into the DB...
      // Publish to MQTT...
      // Forward decoded data to MQTT actor
      mqttActor ! data

      /*
       * In this case, the Listener is the server (using MQTT, inserting into the database, ...)
       * If an actuator should be a NetworkListener, we need to add a notion of role played by the listener
       * or create another class.
       */


      nextActor ! processed // Send the processed data to another actor*
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}