package be.unamur.cattracker.actors

import akka.actor.typed.ActorSystem

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](NetworkListener(null), "network-listener")
  }
}
