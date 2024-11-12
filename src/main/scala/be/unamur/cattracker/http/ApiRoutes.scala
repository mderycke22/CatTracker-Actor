package be.unamur.cattracker.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol.*


trait ApiRoutes {
  val apiRoutes: Route = path("movies" / "heartbeat") {
    get {
      complete("Success")
    }
  }
}
