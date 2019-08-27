package com.github.dedkovva.qs

import akka.http.scaladsl.server._
import akka.http.scaladsl.Http
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.github.dedkovva.qs.rest.Routes
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object Boot {
  private val log = LoggerFactory.getLogger(Boot.getClass)

  implicit val system: ActorSystem = ActorSystem("queuing-service", AppConfig.config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val apiDispatcher: ActorRef = system.actorOf(ApiDispatcher.props(5000 millis), "ApiDispatcher")

  private val routes = Route.handlerFlow(Routes.paths)
  private val http = Http(system)

  def main(args: Array[String]): Unit = {
    import AppConfig.Http
    val binding = http.bindAndHandle(routes, Http.host, Http.port)
    log.info(s"Service online at http://${Http.host}:${Http.port}")
    StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    log.info(s"Service stopped")
  }
}
