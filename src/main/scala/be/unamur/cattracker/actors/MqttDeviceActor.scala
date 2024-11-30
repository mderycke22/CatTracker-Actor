package be.unamur.cattracker.actors

import akka.{Done, NotUsed}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.alpakka.mqtt.scaladsl.{MqttFlow, MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import be.unamur.cattracker.Main.conf
import com.typesafe.config.ConfigFactory
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import akka.stream.alpakka.mqtt.scaladsl.{MqttMessageWithAck, MqttSink, MqttSource}
import akka.stream.scaladsl.MergeHub.source

import scala.concurrent.{Await, Future}

object MqttDeviceActor {

  sealed trait MqttCommand;
  final case class MqttPublish(message: ByteString) extends MqttCommand
  final case class MqttSubscribe(callback: Function[String, Any]) extends MqttCommand

  private final val conf = ConfigFactory.load()
  private val mqttPort = conf.getLong("cat-tracker.mqtt.port")
  private val mqttAddress = conf.getString("cat-tracker.mqtt.ip")
  private val brokerUrl = s"tcp://${mqttAddress}:${mqttPort}"

  private val connectionSettings: MqttConnectionSettings = MqttConnectionSettings(
    brokerUrl,
    "cattracker-backend",
    new MemoryPersistence,
  ).withCleanSession(true)
    .withAutomaticReconnect(true)
  private val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(connectionSettings, MqttQoS.AtLeastOnce)

  def apply(topic: String): Behavior[MqttCommand] = {
      Behaviors.setup { context =>
        import context.executionContext
        implicit val system: ActorSystem[Nothing] = context.system
        Behaviors.receiveMessage {
          case MqttPublish(message) =>

            val mqttMessage = MqttMessage(topic, message)
            Source.single(mqttMessage).runWith(mqttSink)
            Behaviors.same

          case MqttSubscribe(function) =>
            val mqttSource: Source[MqttMessage, Future[Done]] =
              MqttSource.atMostOnce(
                connectionSettings.withClientId(clientId = "cattracker/backend"),
                MqttSubscriptions(Map(topic -> MqttQoS.AtLeastOnce)),
                bufferSize = 8
              )

            mqttSource
              .runForeach(message => function(message.payload.utf8String))

            Behaviors.same
        }
      }
    }


}
