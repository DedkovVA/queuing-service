package com.github.dedkovva.qs.rest

import akka.http.scaladsl.server.{Directives, Route}
import com.github.dedkovva.qs.{JsonTransformer, Util}
import com.github.dedkovva.qs.repository.OrderRepositoryRandom
import com.github.dedkovva.qs.service.OrderService

trait OrderApi extends Directives with JsonTransformer {
  private val orderService = new OrderService(OrderRepositoryRandom)

  val orderPaths: Route = path("order") {
    get {
      parameter('q) { q =>
        val ids = Util.queryToIds(q)
        val result = orderService.fetchOrders(ids)
        complete(result)
      }
    }
  }
}
