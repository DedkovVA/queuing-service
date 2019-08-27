package com.github.dedkovva.qs.repository

import com.github.dedkovva.qs.Types.ProductApiCallSuccess

trait ProductRepository {
  def fetchProducts(customerIds: Seq[String]): ProductApiCallSuccess
}
