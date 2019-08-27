package com.github.dedkovva.qs.service

import com.github.dedkovva.qs.Types.{CompanyProduct, ProductApiCallSuccess}
import com.github.dedkovva.qs.Util
import com.github.dedkovva.qs.repository.ProductRepository

class ProductService(productRepository: ProductRepository) {
  def fetchProducts(customerIds: Seq[String]): ProductApiCallSuccess = {
    val checked = Util.checkIds(customerIds)
    checked.incorrect.map((e: String) => e -> Seq(CompanyProduct.NULL)).toMap ++
      productRepository.fetchProducts(checked.correct)
  }
}
