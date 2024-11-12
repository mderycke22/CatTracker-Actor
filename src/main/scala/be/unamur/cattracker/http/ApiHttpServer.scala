package be.unamur.cattracker.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ApiHttpServer(implicit val system: ActorSystem, val ec: ExecutionContext) extends ApiRoutes {
  def startServer(httpPort: Int): Unit = {
    val serverBinding = Http().newServerAt("localhost", httpPort).bind(apiRoutes)

    serverBinding.map { binding =>
      system.log.info(s"HTTP server started on localhost:${binding.localAddress.getPort}")
    }.recover { case ex =>
      system.log.error("Failed to start the server: " + ex.getMessage)
    }
  }
}
