package com.github.dedkovva.qs.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.dedkovva.qs.ApiDispatcher.HttpRqs
import com.github.dedkovva.qs.{Boot, JsonTransformer}
import com.github.dedkovva.qs.Types.{ApisCallResult, HttpRq}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.blocking
import scala.concurrent.duration._

/**
  * Created by dedkov-va on 07.04.18.
  */
object Routes extends ProductApi with OrderApi with JsonTransformer {
  private val log = LoggerFactory.getLogger(this.getClass)

  val paths: Route =
    path("info") {
      get {
        complete(StatusCodes.OK)
      }
    } ~
    path("dispatchApiCalls") {
      post {
        entity(as[Seq[HttpRq]]) { httpRqs =>
          val duration = 7 seconds
          implicit val timeout: Timeout = Timeout(duration)
          val result = Await.result(blocking((Boot.apiDispatcher ? HttpRqs(httpRqs)).mapTo[ApisCallResult]), duration)
          log.info(s"got result: $result")
          complete(StatusCodes.OK)
        }
      }
    } ~
    productPaths ~
    orderPaths
}
