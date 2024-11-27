package be.unamur.cattracker.http

import akka.actor.typed.*
import akka.http.scaladsl.Http

import scala.concurrent.ExecutionContext

class ApiHttpServer(routes: ApiRoutes)(implicit val system: ActorSystem[Nothing], val ec: ExecutionContext) {
  def startServer(httpPort: Int): Unit = {
    val serverBinding = Http().newServerAt("localhost", httpPort).bind(routes.apiRoutes)

    serverBinding.map { binding =>
      system.log.info(s"HTTP server started on localhost:${binding.localAddress.getPort}")
    }.recover { case ex =>
      system.log.error(s"Failed to start the server: ${ex.getMessage}")
    }
  }
}
