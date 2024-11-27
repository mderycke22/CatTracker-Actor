package be.unamur.cattracker.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.concurrent.{ExecutionContext}

class ApiHttpServer(routes: ApiRoutes)(implicit val system: ActorSystem, val ec: ExecutionContext) {
  def startServer(httpAddress: String, httpPort: Int): Unit = {
    val serverBinding = Http().newServerAt(httpAddress, httpPort).bind(routes.apiRoutes)

    serverBinding.map { binding =>
      system.log.info(s"HTTP server started on localhost:${binding.localAddress.getPort}")
    }.recover { case ex =>
      system.log.error(s"Failed to start the server: ${ex.getMessage}")
    }
  }
}
