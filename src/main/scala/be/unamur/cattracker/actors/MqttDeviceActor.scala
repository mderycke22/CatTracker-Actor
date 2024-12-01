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
  final case class MqttPublish(topic: String, message: ByteString) extends MqttCommand
  final case class MqttSubscribe(callback: Function[String, Any]) extends MqttCommand

  def apply(mqttSink: Sink[MqttMessage, Future[Done]], mqttSource: Source[MqttMessage, Future[Done]]): Behavior[MqttCommand] = {
      Behaviors.setup { context =>
        import context.executionContext
        implicit val system: ActorSystem[Nothing] = context.system
        Behaviors.receiveMessage {
          case MqttPublish(topic, message) =>
            val mqttMessage = MqttMessage(topic, message)
            Source.single(mqttMessage).runWith(mqttSink)
            Behaviors.same

          case MqttSubscribe(function) =>
            mqttSource
              .runForeach(message => function(message.payload.utf8String))

            Behaviors.same
        }
      }
    }


}
