package be.unamur.cattracker.actors


import akka.Done
import akka.actor.Actor
import akka.stream.*
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.*
import akka.stream.scaladsl.*
import akka.util.ByteString
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future

class RemoteMqtt(brokerUrl: String) extends Actor {
  import context.system

  private val connectionSettings = MqttConnectionSettings(
    brokerUrl,
    "test-scala-client",
    new MemoryPersistence
  )
  private val sink: Sink[MqttMessage, Future[Done]] = MqttSink(connectionSettings, MqttQoS.AtLeastOnce)


  def receive: Receive = {
    case data: ByteString =>
      try {
        val topic = "sensors/temperature"
        val message = MqttMessage(topic, data)
        Source(Array(message)).runWith(sink)
        context.system.log.info("Published data to MQTT: {}", data)
      } catch {
        case e: MqttException =>
          context.system.log.error("Failed to publish message to MQTT: {}", e.getMessage)
      }
  }
}