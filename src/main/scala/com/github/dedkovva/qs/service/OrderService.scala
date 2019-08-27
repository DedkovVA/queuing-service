package com.github.dedkovva.qs.service

import com.github.dedkovva.qs.Types.{OrderApiCallSuccess, OrderStatus}
import com.github.dedkovva.qs.Util
import com.github.dedkovva.qs.repository.OrderRepository

class OrderService(orderRepository: OrderRepository) {
  def fetchOrders(customerIds: Seq[String]): OrderApiCallSuccess = {
    val checked = Util.checkIds(customerIds)
    checked.incorrect.map((e: String) => e -> OrderStatus.NULL).toMap ++
      orderRepository.fetchOrders(checked.correct)
  }
}
