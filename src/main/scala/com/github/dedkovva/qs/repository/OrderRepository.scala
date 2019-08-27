package com.github.dedkovva.qs.repository

import com.github.dedkovva.qs.Types.OrderApiCallSuccess

trait OrderRepository {
  def fetchOrders(customerIds: Seq[String]): OrderApiCallSuccess
}
