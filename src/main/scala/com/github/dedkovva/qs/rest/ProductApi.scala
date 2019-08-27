package com.github.dedkovva.qs.rest

import akka.http.scaladsl.server.{Directives, Route}
import com.github.dedkovva.qs.{JsonTransformer, Util}
import com.github.dedkovva.qs.repository.ProductRepositoryRandom
import com.github.dedkovva.qs.service.ProductService

trait ProductApi extends Directives with JsonTransformer {
  private val productService = new ProductService(ProductRepositoryRandom)

  val productPaths: Route = path("product") {
    get {
      parameter('q) { q =>
        val ids = Util.queryToIds(q)
        val result = productService.fetchProducts(ids)
        complete(result)
      }
    }
  }
}
