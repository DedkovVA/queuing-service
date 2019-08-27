package com.github.dedkovva.qs.repository

import com.github.dedkovva.qs.Types.{CompanyProduct, ProductApiCallSuccess}

object ProductRepositoryRandom extends ProductRepository {
  import CompanyProduct._
  private val generator = new RandomThingsGenerator(Seq(ELECTRICITY, EV, GAS), 8)

  override def fetchProducts(customerIds: Seq[String]): ProductApiCallSuccess = {
    generator.evalThings(customerIds)
  }
}
